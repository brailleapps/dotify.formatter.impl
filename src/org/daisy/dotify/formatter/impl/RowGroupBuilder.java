package org.daisy.dotify.formatter.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.daisy.dotify.api.formatter.FormattingTypes.BreakBefore;
import org.daisy.dotify.api.formatter.FormattingTypes.Keep;
import org.daisy.dotify.api.formatter.MarginRegion;
import org.daisy.dotify.api.formatter.RenderingScenario;

class RowGroupBuilder {
	private final LayoutMaster master;
	private final BlockSequence seq;
	private final BlockContext bc;
	private final UnwriteableAreaInfo uai;

	RowGroupBuilder(LayoutMaster master, BlockSequence seq, BlockContext blockContext, UnwriteableAreaInfo uai) {
		this.seq = seq;
		this.master = master;

		//TODO: This assumes that all page templates have margin regions that are of the same width 
		final int mw = getTotalMarginRegionWidth(); 
		bc = new BlockContext(seq.getLayoutMaster().getFlowWidth() - mw, blockContext.getRefs(), blockContext.getContext(), blockContext.getFcontext());
		this.uai = uai;
	}
	
	private int getTotalMarginRegionWidth() {
		int mw = 0;
		for (MarginRegion mr : master.getTemplate(1).getLeftMarginRegion()) {
			mw += mr.getWidth();
		}
		for (MarginRegion mr : master.getTemplate(1).getRightMarginRegion()) {
			mw += mr.getWidth();
		}
		return mw;
	}

	private static void setProperties(RowGroup.Builder rgb, AbstractBlockContentManager bcm, Block g) {
		if (!"".equals(g.getIdentifier())) { 
			rgb.identifier(g.getIdentifier());
		}
		rgb.markers(bcm.getGroupMarkers());
		rgb.anchors(bcm.getGroupAnchors());
		rgb.keepWithNextSheets(g.getKeepWithNextSheets());
		rgb.keepWithPreviousSheets(g.getKeepWithPreviousSheets());
	}
	
	
	ListIterator<RowGroupSequence> getResult() {
		return new ListIterator<RowGroupSequence>() {
			
			int cursor = 0; // cursor for outside
			PageSequenceRecorder rec = new PageSequenceRecorder();
			RowGroupSequence next;
			Iterator<RowGroupSequence> nexts;
			List<Block> filteredSeq = new ArrayList<>();
			State state = new State();
			State mark = null;
			String recMark = "mark1";
			State nextMark = null; // becomes mark when cursor is incremented
			String nextRecMark = "mark2";
			
			class State implements Cloneable {
				int cursor = 0; // cursor for computation
				int nextBlock = 0; // index in seq
				int nextFilteredBlock = 0; // index in filteredSeq
				public Object clone() {
					try {
						return super.clone();
					} catch (CloneNotSupportedException e) {
						throw new InternalError("coding error");
					}
				}
			}
			
			public int nextIndex() {
				return cursor;
			}
			
			public int previousIndex() {
				return cursor-1;
			}
			
			public boolean hasNext() {
				if (next != null || (nexts != null && nexts.hasNext())) {
					return true;
				} else {
					if (nextMark == null) {
						nextMark = (State)state.clone();
						rec.saveState(nextRecMark);
					}
					try {
						nexts = computeNext();
						next = nexts.next();
						return true;
					} catch (NoSuchElementException e) {
						return false;
					} catch (IllegalStateException e) {
						throw new RuntimeException("coding error");
					}
				}
			}
			
			public boolean hasPrevious() {
				return cursor != 0;
			}
			
			public RowGroupSequence next() throws NoSuchElementException {
				if (next == null && (nexts == null || !nexts.hasNext())) {
					if (nextMark == null) {
						nextMark = (State)state.clone();
						rec.saveState(nextRecMark);
					}
					try {
						nexts = computeNext();
						next = nexts.next();
					} catch (IllegalStateException e) {
						throw new RuntimeException("coding error");
					}
				}
				cursor++;
				mark = nextMark;
				nextMark = null;
				String tmp = recMark;
				recMark = nextRecMark;
				nextRecMark = tmp;
				RowGroupSequence r = next;
				next = null;
				return r;
			}
			
			/**
			 * @throws IllegalStateException if recomputing the previous RowGroupSequence results in a
			 * different scenario being selected than before, and one or more RowGroupSequences before the
			 * previous one contain rows that belong to the old scenario. Upon getting this exception the
			 * ListIterator may not be used again.
			 */
			public RowGroupSequence previous() throws IllegalStateException, NoSuchElementException {
				if (cursor == 0) {
					throw new NoSuchElementException();
				}
				if (mark == null) {
					throw new IllegalStateException();
				}
				state = (State)mark.clone();
				rec.restoreState(recMark);
				cursor--;
				try {
					nexts = computeNext();
					next = nexts.next();
				} catch (NoSuchElementException e) {
					throw new IllegalStateException();
				}
				nextMark = mark;
				mark = null;
				String tmp = nextRecMark;
				nextRecMark = recMark;
				recMark = tmp;
				return next;
			}
			
			/**
			 * @throws NoSuchElementException if no iterator of one or more items can be returned.
			 */
			Iterator<RowGroupSequence> computeNext() throws IllegalStateException, NoSuchElementException {
				Map<RenderingScenario,List<Block>> scenarios = null;
				Map<RenderingScenario,List<Integer>> scenarioEndBlocks = null;
				RenderingScenario currentScenario = null;
				int nextBlockInScenario = 0;
				while (true) {
					Block g = state.nextBlock < seq.size() ? seq.elementAt(state.nextBlock) : null;
					RenderingScenario scenario = g != null ? g.getRenderingScenario() : null;
					if (currentScenario != null && scenario == null) {
						RenderingScenario best = rec.endScenarios();
						List<Integer> bestEndBlocks = scenarioEndBlocks.get(best);
						int cursorAdvance = 0;
						int blockBeforeScenarios = state.nextFilteredBlock - 1;
						for (Block b : scenarios.get(best)) {
							if (state.cursor + cursorAdvance < cursor) {
								if (filteredSeq.get(state.nextFilteredBlock) != b) {
									throw new IllegalStateException();
								}
							} else if (state.nextFilteredBlock == filteredSeq.size()) {
								filteredSeq.add(b);
							} else {
								filteredSeq.set(state.nextFilteredBlock, b);
							}
							if (cursorAdvance < bestEndBlocks.size()
							    && state.nextFilteredBlock == blockBeforeScenarios + 1 + bestEndBlocks.get(cursorAdvance)) {
								cursorAdvance++;
							}
							state.nextFilteredBlock++;
						}
					}
					Iterator<RowGroupSequence> iter = null;
					if (currentScenario != scenario && scenario != null) {
						if (currentScenario == null) {
							iter = rec.getResult(state.cursor); // needed when startNew is true, but needs to be
							                                    // computed before rec.startScenario (before startNew is
							                                    // known)
						}
						rec.startScenario(scenario); // needs to be called before rec.isDataGroupsEmpty
					}
					boolean startNew = g != null ?
						rec.isDataGroupsEmpty()
						|| (g.getBreakBeforeType()==BreakBefore.PAGE && !rec.isDataEmpty())
						|| g.getVerticalPosition()!=null
						: false;
					if (g == null || (startNew && (currentScenario == null || scenario == null))) {
						List<RowGroupSequence> list = new ArrayList<>(); {
							if (iter == null) {
								iter = rec.getResult(state.cursor);
							}
							while (iter.hasNext()) {
								list.add(iter.next());
								state.cursor++;
							}
						}
						Iterator<RowGroupSequence> ret = list.iterator();
						if (ret.hasNext()) {
							return ret;
						} else if (g == null) {
							throw new NoSuchElementException();
						}
					}
					if (g != null) {
						if (currentScenario != scenario && scenario != null) {
							if (currentScenario == null) {
								scenarios = new HashMap<>();
								scenarioEndBlocks = new HashMap<>();
							}
							scenarios.put(scenario, new ArrayList<Block>());
							scenarioEndBlocks.put(scenario, new ArrayList<Integer>());
							nextBlockInScenario = 0;
						}
						currentScenario = scenario;
						try {
							AbstractBlockContentManager bcm = g.getBlockContentManager(bc, uai);
							if (startNew) {
								rec.newRowGroupSequence(g.getVerticalPosition(), new RowImpl("", bcm.getLeftMarginParent(), bcm.getRightMarginParent()));
								rec.setKeepWithNext(-1);
							}
							rec.processBlock(bcm);
							List<RowGroup> store = new ArrayList<>();
							List<RowImpl> rl1 = bcm.getCollapsiblePreContentRows();
							if (!rl1.isEmpty()) {
								store.add(new RowGroup.Builder(master.getRowSpacing(), rl1).
														collapsible(true).skippable(false).breakable(false).build());
							}
							List<RowImpl> rl2 = bcm.getInnerPreContentRows();
							if (!rl2.isEmpty()) {
								store.add(new RowGroup.Builder(master.getRowSpacing(), rl2).
														collapsible(false).skippable(false).breakable(false).build());
							}
							if (bcm.getRowCount()==0) { //TODO: Does this interfere with collapsing margins? 
								if (!bcm.getGroupAnchors().isEmpty() || !bcm.getGroupMarkers().isEmpty() || !"".equals(g.getIdentifier())
										|| g.getKeepWithNextSheets()>0 || g.getKeepWithPreviousSheets()>0 ) {
									RowGroup.Builder rgb = new RowGroup.Builder(master.getRowSpacing(), new ArrayList<RowImpl>());
									setProperties(rgb, bcm, g);
									store.add(rgb.build());
								}
							}
							int i = 0;
							List<RowImpl> rl3 = bcm.getPostContentRows();
							OrphanWidowControl owc = new OrphanWidowControl(g.getRowDataProperties().getOrphans(),
																			g.getRowDataProperties().getWidows(), 
																			bcm.getRowCount());
							for (RowImpl r : bcm) {
								i++;
								r.setAdjustedForMargin(true);
								if (i==bcm.getRowCount()) {
									//we're at the last line, this should be kept with the next block's first line
									rec.setKeepWithNext(g.getKeepWithNext());
								}
								RowGroup.Builder rgb = new RowGroup.Builder(master.getRowSpacing()).add(r).
										collapsible(false).skippable(false).breakable(
												r.allowsBreakAfter()&&
												owc.allowsBreakAfter(i-1)&&
												rec.getKeepWithNext()<=0 &&
												(Keep.AUTO==g.getKeepType() || i==bcm.getRowCount()) &&
												(i<bcm.getRowCount() || rl3.isEmpty())
												);
								if (i==1) { //First item
									setProperties(rgb, bcm, g);
								}
								store.add(rgb.build());
								rec.setKeepWithNext(rec.getKeepWithNext()-1);
							}
							if (!rl3.isEmpty()) {
								store.add(new RowGroup.Builder(master.getRowSpacing(), rl3).
									collapsible(false).skippable(false).breakable(rec.getKeepWithNext()<0).build());
							}
							List<RowImpl> rl4 = bcm.getSkippablePostContentRows();
							if (!rl4.isEmpty()) {
								store.add(new RowGroup.Builder(master.getRowSpacing(), rl4).
									collapsible(true).skippable(true).breakable(rec.getKeepWithNext()<0).build());
							}
							if (store.isEmpty() && !rec.isDataGroupsEmpty()) {
								RowGroup gx = rec.currentSequence().currentGroup();
								if (gx!=null && gx.getAvoidVolumeBreakAfterPriority()==g.getAvoidVolumeBreakInsidePriority()
										&&gx.getAvoidVolumeBreakAfterPriority()!=g.getAvoidVolumeBreakAfterPriority()) {
									gx.setAvoidVolumeBreakAfterPriority(g.getAvoidVolumeBreakAfterPriority());
								}
							} else {
								for (int j=0; j<store.size(); j++) {
									RowGroup b = store.get(j);
									if (j==store.size()-1) {
										b.setAvoidVolumeBreakAfterPriority(g.getAvoidVolumeBreakAfterPriority());
									} else {
										b.setAvoidVolumeBreakAfterPriority(g.getAvoidVolumeBreakInsidePriority());
									}
									rec.addRowGroup(b);
								}
							}
						} catch (Exception e) {
							rec.invalidateScenario(e);
						}
						state.nextBlock++;
						if (currentScenario != null) {
							if (startNew) {
								scenarioEndBlocks.get(currentScenario).add(nextBlockInScenario - 1);
							}
							nextBlockInScenario++;
						} else {
							if (state.cursor < cursor) {
								if (filteredSeq.get(state.nextFilteredBlock) != g) {
									throw new IllegalStateException();
								}
							} else if (state.nextFilteredBlock == filteredSeq.size()) {
								filteredSeq.add(g);
							} else {
								filteredSeq.set(state.nextFilteredBlock, g);
							}
							state.nextFilteredBlock++;
						}
					}
				}
			}
			
			public void add(RowGroupSequence e) {
				throw new UnsupportedOperationException("unmodifiable");
			}
			
			public void remove() {
				throw new UnsupportedOperationException("unmodifiable");
			}
			
			public void set(RowGroupSequence e) {
				throw new UnsupportedOperationException("unmodifiable");
			}
		};
	}

}
