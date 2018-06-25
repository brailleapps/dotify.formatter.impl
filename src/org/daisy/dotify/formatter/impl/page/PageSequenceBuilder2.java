package org.daisy.dotify.formatter.impl.page;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.daisy.dotify.api.formatter.BlockPosition;
import org.daisy.dotify.api.formatter.FallbackRule;
import org.daisy.dotify.api.formatter.FormattingTypes.BreakBefore;
import org.daisy.dotify.api.formatter.MarginRegion;
import org.daisy.dotify.api.formatter.MarkerIndicatorRegion;
import org.daisy.dotify.api.formatter.PageAreaProperties;
import org.daisy.dotify.api.formatter.RenameFallbackRule;
import org.daisy.dotify.api.formatter.TransitionBuilderProperties.ApplicationRange;
import org.daisy.dotify.api.translator.Translatable;
import org.daisy.dotify.api.translator.TranslationException;
import org.daisy.dotify.common.splitter.SplitPoint;
import org.daisy.dotify.common.splitter.SplitPointCost;
import org.daisy.dotify.common.splitter.SplitPointDataSource;
import org.daisy.dotify.common.splitter.SplitPointDataList;
import org.daisy.dotify.common.splitter.SplitPointHandler;
import org.daisy.dotify.common.splitter.SplitPointSpecification;
import org.daisy.dotify.common.splitter.StandardSplitOption;
import org.daisy.dotify.common.splitter.Supplements;
import org.daisy.dotify.formatter.impl.core.Block;
import org.daisy.dotify.formatter.impl.core.BlockContext;
import org.daisy.dotify.formatter.impl.core.ContentCollectionImpl;
import org.daisy.dotify.formatter.impl.core.FormatterContext;
import org.daisy.dotify.formatter.impl.core.LayoutMaster;
import org.daisy.dotify.formatter.impl.core.PaginatorException;
import org.daisy.dotify.formatter.impl.core.TransitionContent;
import org.daisy.dotify.formatter.impl.core.TransitionContent.Type;
import org.daisy.dotify.formatter.impl.datatype.VolumeKeepPriority;
import org.daisy.dotify.formatter.impl.row.AbstractBlockContentManager;
import org.daisy.dotify.formatter.impl.row.MarginProperties;
import org.daisy.dotify.formatter.impl.row.RowImpl;
import org.daisy.dotify.formatter.impl.search.CrossReferenceHandler;
import org.daisy.dotify.formatter.impl.search.DefaultContext;
import org.daisy.dotify.formatter.impl.search.DocumentSpace;
import org.daisy.dotify.formatter.impl.search.PageDetails;
import org.daisy.dotify.formatter.impl.search.PageId;
import org.daisy.dotify.formatter.impl.search.SequenceId;
import org.daisy.dotify.formatter.impl.search.TransitionProperties;

public class PageSequenceBuilder2 {
	private final FormatterContext context;
	private final PageAreaContent staticAreaContent;
	private final PageAreaProperties areaProps;

	private final ContentCollectionImpl collection;
	private BlockContext initialContext;
	private final CollectionData cd;
	private final LayoutMaster master;
	private final List<RowGroupSequence> dataGroups;
	private final FieldResolver fieldResolver;
	private final SequenceId seqId;
	private final SplitPointHandler<RowGroup> sph;

	private boolean force;
	private RowGroupDataSource data;

	private int keepNextSheets;
	private int pageCount = 0;
	private int dataGroupsIndex;
	private boolean nextEmpty = false;

	//From view, temporary
	private final int fromIndex;
	private int toIndex;
	
	public PageSequenceBuilder2(int fromIndex, LayoutMaster master, int pageOffset, BlockSequence seq, FormatterContext context, DefaultContext rcontext, int sequenceId) {
		this.fromIndex = fromIndex;
		this.toIndex = fromIndex;
		this.master = master;
		this.context = context;
		this.sph = new SplitPointHandler<>();
		this.areaProps = seq.getLayoutMaster().getPageArea();
		if (this.areaProps!=null) {
			this.collection = context.getCollections().get(areaProps.getCollectionId());
		} else {
			this.collection = null;
		}
		keepNextSheets = 0;
		
		initialContext = BlockContext.from(rcontext)
				.flowWidth(seq.getLayoutMaster().getFlowWidth())
				.formatterContext(context)
				.build();
		// BlockContext used for rendering before and after sequences in page-area (CrossReferenceHandler used for getting page number)
		this.staticAreaContent = new PageAreaContent(seq.getLayoutMaster().getPageAreaBuilder(), initialContext);
		//For the scenario processing, it is assumed that all page templates have margin regions that are of the same width.
		//However, it is unlikely to have a big impact on the selection.
		BlockContext bc = BlockContext.from(initialContext)
				.flowWidth(master.getFlowWidth() - master.getTemplate(1).getTotalMarginRegionWidth())
				.build();
		this.dataGroups = seq.selectScenario(master, bc, true);
		// BlockContext used for rendering collection items (CrossReferenceHandler used for getting page number)
		this.cd = new CollectionData(staticAreaContent, initialContext, master, collection);
		this.dataGroupsIndex = 0;
		this.seqId = new SequenceId(sequenceId, new DocumentSpace(rcontext.getSpace(), rcontext.getCurrentVolume()));
		PageDetails details = new PageDetails(master.duplex(), new PageId(pageCount, getGlobalStartIndex(), seqId), pageOffset);
		this.fieldResolver = new FieldResolver(master, context, () -> getContext().getRefs(), details);
	}

	public PageSequenceBuilder2(PageSequenceBuilder2 template) {
		this.context = template.context;
		this.staticAreaContent = template.staticAreaContent;
		this.areaProps = template.areaProps;
		this.collection = template.collection;
		this.initialContext = template.initialContext;
		this.master = template.master;
		this.dataGroups = template.dataGroups;
		this.cd = template.cd;
		this.dataGroupsIndex = template.dataGroupsIndex;
		this.fieldResolver = template.fieldResolver;
		this.seqId = template.seqId;
		this.sph = template.sph;
		this.force = template.force;
		this.data = RowGroupDataSource.copyUnlessNull(template.data);
		this.keepNextSheets = template.keepNextSheets;
		this.pageCount = template.pageCount;
		this.nextEmpty = template.nextEmpty;
		this.fromIndex = template.fromIndex;
		this.toIndex = template.toIndex;
	}
	
	public static PageSequenceBuilder2 copyUnlessNull(PageSequenceBuilder2 template) {
		return template==null?null:new PageSequenceBuilder2(template);
	}
	
	public BlockContext getContext() {
		if (data != null)
			return data.getContext();
		else
			return initialContext;
	}
	
	// FIXME: make immutable
	public void modifyContext(Consumer<? super BlockContext.Builder> modifier) {
		if (data != null) {
			data.modifyContext(modifier);
		} else {
			BlockContext.Builder b = initialContext.builder();
			modifier.accept(b);
			initialContext = b.build();
		}
	}
	
	private void modifyRefs(Consumer<CrossReferenceHandler.Builder> modifier) {
		modifyContext(c -> modifier.accept(c.getRefs()));
	}
	
	/**
	 * Gets a new PageId representing the next page in this sequence.
	 * @param offset the offset
	 * @return returns the next page Id
	 */
	public PageId nextPageId(int offset) {
		return new PageId(pageCount+offset, getGlobalStartIndex(), seqId);
	}

	private PageImpl newPage(int pageNumberOffset) {
		PageDetails details = new PageDetails(master.duplex(), new PageId(pageCount, getGlobalStartIndex(), seqId), pageNumberOffset);
		PageImpl ret = new PageImpl(fieldResolver, details, master, context, staticAreaContent);
		pageCount ++;
		if (keepNextSheets>0) {
			ret.setAllowsVolumeBreak(false);
		}
		if (!master.duplex() || pageCount%2==0) {
			if (keepNextSheets>0) {
				keepNextSheets--;
			}
		}
		return ret;
	}

	private void newRow(PageImpl p, RowImpl row) {
		if (p.spaceUsedOnPage(1) > p.getFlowHeight()) {
			throw new RuntimeException("Error in code.");
			//newPage();
		}
		p.newRow(row);
	}

	public boolean hasNext() {
		return dataGroupsIndex<dataGroups.size() || (data!=null && !data.isEmpty());
	}
	
	public PageImpl nextPage(int pageNumberOffset, boolean hyphenateLastLine, Optional<TransitionContent> transitionContent) throws PaginatorException, RestartPaginationException // pagination must be restarted in PageStructBuilder.paginateInner
	{
		PageImpl ret = nextPageInner(pageNumberOffset, hyphenateLastLine, transitionContent);
		modifyRefs(refs -> {
				refs.setPageDetails(ret.getDetails());
				for (String id : ret.getIdentifiers()) {
					refs.setPageNumber(id, ret.getPageNumber());
				}
				//This is for pre/post volume contents, where the volume number is known
				Integer volume = getContext().getCurrentVolume();
				if (volume!=null) {
					for (String id : ret.getIdentifiers()) {
						refs.setVolumeNumber(id, volume);
					}
				}
			});
		toIndex++;
		return ret;
	}

	private PageImpl nextPageInner(int pageNumberOffset, boolean hyphenateLastLine, Optional<TransitionContent> transitionContent) throws PaginatorException, RestartPaginationException // pagination must be restarted in PageStructBuilder.paginateInner
	{
		PageImpl current = newPage(pageNumberOffset);
		if (nextEmpty) {
			nextEmpty = false;
			return current;
		}
		while (dataGroupsIndex<dataGroups.size() || (data!=null && !data.isEmpty())) {
			if ((data==null || data.isEmpty()) && dataGroupsIndex<dataGroups.size()) {
				//pick up next group
				RowGroupSequence rgs = dataGroups.get(dataGroupsIndex);
				//TODO: This assumes that all page templates have margin regions that are of the same width
				BlockContext bc = BlockContext.from(getContext())
						.flowWidth(master.getFlowWidth() - master.getTemplate(current.getPageNumber()).getTotalMarginRegionWidth())
						.build();
				data = new RowGroupDataSource(master, bc, rgs.getBlocks(), rgs.getBreakBefore(), rgs.getVerticalSpacing(), cd);
				dataGroupsIndex++;
				if (data.getVerticalSpacing()!=null) {
					VerticalSpacing vSpacing = data.getVerticalSpacing();
					float size; {
						size = 0;
						for (SplitPointDataSource.Iterator<RowGroup> it = data.iterator(); it.hasNext();) {
							size += it.next(false).getUnitSize();
						}
					}
					int pos = calculateVerticalSpace(current, vSpacing.getBlockPosition(), (int)Math.ceil(size));
					for (int i = 0; i < pos; i++) {
						RowImpl ri = vSpacing.getEmptyRow();
						newRow(current, new RowImpl(ri.getChars(), ri.getLeftMargin(), ri.getRightMargin()));
					}
				}
				force = false;
			}
			modifyContext(ctxt -> {
					ctxt.currentPage(current.getDetails().getPageNumber())
					    .flowWidth(master.getFlowWidth() - master.getTemplate(current.getPageNumber()).getTotalMarginRegionWidth());
				}
			);
			Optional<Boolean> blockBoundary = Optional.empty();
			if (!data.isEmpty()) {
				int index = SplitPointHandler.findLeading(data);
				SplitPoint<RowGroup> sl = SplitPointHandler.skipLeading(data, index);
				for (RowGroup rg : sl.getDiscarded()) {
					addProperties(current, rg);
				}
				data = (RowGroupDataSource)sl.getTail();
				SplitPointDataSource<RowGroup> seqTransitionText = transitionContent.isPresent()
						? new RowGroupDataSource(master, getContext(), transitionContent.get().getInSequence(), BreakBefore.AUTO, null, cd)
						: SplitPointDataList.emptyList();
				float transitionHeight;
				SplitPointSpecification spec;
				boolean addTransition = true;
				if (transitionContent.isPresent() && transitionContent.get().getType()==Type.INTERRUPT) {
					// Subtract the height of the transition text from the available height.
					// We need to account for the last unit size here (because this is the last unit) instead of the one below.
					// The transition text may have a smaller row spacing than the last row of the text flow, for example:
					// Text rows: 		X-X-X--|
					// Transition rows:		 X-|
					// This transition doesn't fit, because the last row of the text flow takes up three rows, not just one (which it would
					// if a transition didn't follow).
					transitionHeight = height(seqTransitionText, true);
					float flowHeight = current.getFlowHeight() - transitionHeight;
					SplitPointCost<RowGroup> cost = (RowGroup unit, int in, int limit)->{
						VolumeKeepPriority volumeBreakPriority = 
								unit.getAvoidVolumeBreakAfterPriority();
						double volBreakCost = // 0-9:
								10-(volumeBreakPriority.orElse(10));
						// not breakable gets "series" 21
						// breakable, but not last gets "series" 11-20
						// breakable and last gets "series" 1-10
						return (unit.isBreakable()?
									//prefer new block, then lower volume priority cost
								(unit.isLastRowGroupInBlock()?1:11) + volBreakCost 
									:21 // because 11 + 9 = 20
								)*limit-in;
					};
					// Finding from the full height
					spec = sph.find(current.getFlowHeight(), data, cost, force?StandardSplitOption.ALLOW_FORCE:null);
					SplitPoint<RowGroup> x = sph.split(spec, data);
					// If the tail is empty, there's no need for a transition
					// If there isn't a transition between blocks available, don't insert the text
					blockBoundary = Optional.of(hasBlockInScope(x.getHead(), flowHeight));
					if (!x.getTail().isEmpty() && blockBoundary.get()) {
						// Find the best break point with the new limit
						spec = sph.find(flowHeight, data, cost, transitionContent.isPresent()?StandardSplitOption.NO_LAST_UNIT_SIZE:null, force?StandardSplitOption.ALLOW_FORCE:null);
					} else {
						addTransition = false;
					}
				} else {
					// Either RESUME, or no transition on this page.
					transitionHeight = height(seqTransitionText, false);
					float flowHeight = current.getFlowHeight() - transitionHeight;
					spec = sph.find(flowHeight, data, force?StandardSplitOption.ALLOW_FORCE:null);
				}
				// Now apply the information to the live data
				data.setAllowHyphenateLastLine(hyphenateLastLine);
				SplitPoint<RowGroup> res = sph.split(spec, data);
				data.setAllowHyphenateLastLine(true);
				if (res.getHead().size()==0 && force) {
					// FIXME: isn't it better to check spec.getIndex() before split?
					if (firstUnitHasSupplements(data) && hasPageAreaCollection()) {
						reassignCollection();
					} else {
						throw new RuntimeException("A layout unit was too big for the page.");
					}
				}
				for (RowGroup rg : res.getSupplements()) {
					current.addToPageArea(rg.getRows());
				}
				force = res.getHead().size()==0;
				data = (RowGroupDataSource)res.getTail();
				List<RowGroup> head;
				if (addTransition && transitionContent.isPresent()) {
					// recompute transition with new context
					seqTransitionText = new RowGroupDataSource(master, getContext(), transitionContent.get().getInSequence(), BreakBefore.AUTO, null, cd);
					if (transitionHeight != height(seqTransitionText, transitionContent.get().getType()==Type.INTERRUPT))
						throw new RuntimeException();
					if (transitionContent.get().getType()==TransitionContent.Type.INTERRUPT) {
						head = new ArrayList<>(res.getHead());
						SplitPointDataSource.Iterator<RowGroup> it = seqTransitionText.iterator();
						while (it.hasNext()) {
							head.add(it.next(false));
						}
						modifyContext(
							c -> c.refs(((RowGroupDataSource)it.iterable()).getContext().getRefs())
						);
					} else if (transitionContent.get().getType()==TransitionContent.Type.RESUME) {
						head = new ArrayList<>();
						SplitPointDataSource.Iterator<RowGroup> it = seqTransitionText.iterator();
						while (it.hasNext()) {
							head.add(it.next(false));
						}
						modifyContext(
							c -> c.refs(((RowGroupDataSource)it.iterable()).getContext().getRefs())
						);
						head.addAll(res.getHead());
					} else {
						head = res.getHead();
					}
				} else {
					head = res.getHead();
				}
				addRows(head, current);
				current.setAvoidVolumeBreakAfter(getVolumeKeepPriority(res.getDiscarded(), getVolumeKeepPriority(res.getHead(), VolumeKeepPriority.empty())));
				if (context.getTransitionBuilder().getProperties().getApplicationRange()!=ApplicationRange.NONE) {
					// no need to do this, unless there is an active transition builder
					boolean hasBlockBoundary = blockBoundary.isPresent()?blockBoundary.get():res.getHead().stream().filter(r->r.isLastRowGroupInBlock()).findFirst().isPresent();
					modifyRefs(
						refs -> refs.setTransitionProperties(current.getDetails().getPageId(),
						                                     new TransitionProperties(current.getAvoidVolumeBreakAfter(), hasBlockBoundary)));
					
				}
				for (RowGroup rg : res.getDiscarded()) {
					addProperties(current, rg);
				}
				if (hasPageAreaCollection() && current.pageAreaSpaceNeeded() > master.getPageArea().getMaxHeight()) {
					reassignCollection();
				}
				if (!data.isEmpty()) {
					return current;
				} else if (current!=null && dataGroupsIndex<dataGroups.size()) {
					BreakBefore nextStart = dataGroups.get(dataGroupsIndex).getBreakBefore();
					if (nextStart!=BreakBefore.AUTO) {
						if (nextStart == BreakBefore.SHEET && master.duplex() && pageCount%2==1) {
							nextEmpty = true;
						}
						return current;
					}
				}
			}
		}
		return current;
	}
	
	/**
	 * Returns true if there is a block boundary before or at the specified limit.
	 * @param groups the data
	 * @param limit the size limit
	 * @return true if there is a block boundary within the limit
	 */
	private static boolean hasBlockInScope(List<RowGroup> groups, double limit) {
		// TODO: In Java 9, use takeWhile
		//return groups.stream().limit((int)Math.ceil(limit)).filter(r->r.isLastRowGroupInBlock()).findFirst().isPresent();
		double h = 0;
		Iterator<RowGroup> rg = groups.iterator();
		RowGroup r;
		while (rg.hasNext()) {
			r=rg.next();
			h += rg.hasNext()?r.getUnitSize():r.getLastUnitSize();
			if (h>limit) {
				// we've passed the limit
				return false;
			} else if (r.isLastRowGroupInBlock()) {
				return true;
			}
		}
		return false;
	}
	
	private static float height(SplitPointDataSource<RowGroup> rg, boolean useLastUnitSize) {
		if (rg.isEmpty()) {
			return 0;
		} else {
			float ret = 0;
			SplitPointDataSource.Iterator<RowGroup> ri = rg.iterator();
			while (ri.hasNext()) {
				RowGroup r = ri.next(false);
				ret += useLastUnitSize&&!ri.hasNext()?r.getLastUnitSize():r.getUnitSize();
			}
			return ret;
		}
	}
	
	private void addRows(List<RowGroup> head, PageImpl p) {
		int i = head.size();
		for (RowGroup rg : head) {
			i--;
			addProperties(p, rg);
			List<RowImpl> rows = rg.getRows();
			int j = rows.size();
			for (RowImpl r : rows) {
				j--;
				if (r.shouldAdjustForMargin() || (i == 0 && j == 0)) {
					// clone the row as not to append the margins twice
					RowImpl.Builder b = new RowImpl.Builder(r);
					if (r.shouldAdjustForMargin()) {
						MarkerRef rf = r::hasMarkerWithName;
						MarginProperties margin = r.getLeftMargin();
						for (MarginRegion mr : p.getPageTemplate().getLeftMarginRegion()) {
							margin = getMarginRegionValue(mr, rf, false).append(margin);
						}
						b.leftMargin(margin);
						margin = r.getRightMargin();
						for (MarginRegion mr : p.getPageTemplate().getRightMarginRegion()) {
							margin = margin.append(getMarginRegionValue(mr, rf, true));
						}
						b.rightMargin(margin);
					}
					if (i == 0 && j == 0) {
						// this is the last row; set row spacing to 1 because this is how sph treated it
						b.rowSpacing(null);
					}
					p.newRow(b.build());
				} else {
					p.newRow(r);
				}
			}
		}
	}
	
	private VolumeKeepPriority getVolumeKeepPriority(List<RowGroup> list, VolumeKeepPriority def) {		
		if (!list.isEmpty()) {
			if (context.getTransitionBuilder().getProperties().getApplicationRange()==ApplicationRange.NONE) {
				return list.get(list.size()-1).getAvoidVolumeBreakAfterPriority();
			} else {
				// we want the highest value (lowest priority) to maximize the chance that this page is used
				// when finding the break point
				return list.stream().map(v->v.getAvoidVolumeBreakAfterPriority())
						.max(VolumeKeepPriority::compare)
						.orElse(VolumeKeepPriority.empty());
			}
		} else {
			return def;
		}
	}
	
	private boolean firstUnitHasSupplements(SplitPointDataSource<?> spd) {
		if (spd.isEmpty()) {
			return false;
		} else {
			return !spd.iterator().next(false).getSupplementaryIDs().isEmpty();
		}
	}
	
	private boolean hasPageAreaCollection() {
		return master.getPageArea()!=null && collection!=null;
	}
	
	@FunctionalInterface
	interface MarkerRef {
		boolean hasMarkerWithName(String name);
	}
	
	private MarginProperties getMarginRegionValue(MarginRegion mr, MarkerRef r, boolean rightSide) throws PaginatorException {
		String ret = "";
		int w = mr.getWidth();
		if (mr instanceof MarkerIndicatorRegion) {
			ret = firstMarkerForRow(r, (MarkerIndicatorRegion)mr);
			if (ret.length()>0) {
				try {
					ret = context.getDefaultTranslator().translate(Translatable.text(context.getConfiguration().isMarkingCapitalLetters()?ret:ret.toLowerCase()).build()).getTranslatedRemainder();
				} catch (TranslationException e) {
					throw new PaginatorException("Failed to translate: " + ret, e);
				}
			}
			boolean spaceOnly = ret.length()==0;
			if (ret.length()<w) {
				StringBuilder sb = new StringBuilder();
				if (rightSide) {
					while (sb.length()<w-ret.length()) { sb.append(context.getSpaceCharacter()); }
					sb.append(ret);
				} else {
					sb.append(ret);				
					while (sb.length()<w) { sb.append(context.getSpaceCharacter()); }
				}
				ret = sb.toString();
			} else if (ret.length()>w) {
				throw new PaginatorException("Cannot fit " + ret + " into a margin-region of size "+ mr.getWidth());
			}
			return new MarginProperties(ret, spaceOnly);
		} else {
			throw new PaginatorException("Unsupported margin-region type: " + mr.getClass().getName());
		}
	}
	
	private String firstMarkerForRow(MarkerRef r, MarkerIndicatorRegion mrr) {
		return mrr.getIndicators().stream()
				.filter(mi -> r.hasMarkerWithName(mi.getName()))
				.map(mi -> mi.getIndicator())
				.findFirst().orElse("");
	}
	
	private void addProperties(PageImpl p, RowGroup rg) {
		p.addIdentifiers(rg.getIdentifiers());
		p.addMarkers(rg.getMarkers());
		//TODO: addGroupAnchors
		keepNextSheets = Math.max(rg.getKeepWithNextSheets(), keepNextSheets);
		if (keepNextSheets>0) {
			p.setAllowsVolumeBreak(false);
		}
		p.setKeepWithPreviousSheets(rg.getKeepWithPreviousSheets());
	}
	
	private void reassignCollection() throws PaginatorException {
		//reassign collection
		if (areaProps!=null) {
			int i = 0;
			for (FallbackRule r : areaProps.getFallbackRules()) {
				i++;
				if (r instanceof RenameFallbackRule) {
					ContentCollectionImpl reassigned = context.getCollections().remove(r.applyToCollection());
					if (context.getCollections().put(((RenameFallbackRule)r).getToCollection(), reassigned)!=null) {
						throw new PaginatorException("Fallback id already in use:" + ((RenameFallbackRule)r).getToCollection());
					}							
				} else {
					throw new PaginatorException("Unknown fallback rule: " + r);
				}
			}
			if (i==0) {
				throw new PaginatorException("Failed to fit collection '" + areaProps.getCollectionId() + "' within the page-area boundaries, and no fallback was defined.");
			}
		}
		throw new RestartPaginationException();
	}
	
	static class CollectionData implements Supplements<RowGroup> {
		private final BlockContext c;
		private final PageAreaContent staticAreaContent;
		private final LayoutMaster master;
		private final ContentCollectionImpl collection;
		
		private CollectionData(PageAreaContent staticAreaContent, BlockContext c, LayoutMaster master, ContentCollectionImpl collection) {
			this.c = c;
			this.staticAreaContent = staticAreaContent;
			this.master = master;
			this.collection = collection;
		}
		
		@Override
		public double getOverhead() {
			return PageImpl.rowsNeeded(staticAreaContent.getBefore(), master.getRowSpacing()) 
					+ PageImpl.rowsNeeded(staticAreaContent.getAfter(), master.getRowSpacing());
		}

		@Override
		public RowGroup get(String id) {
			if (collection!=null) {
				RowGroup.Builder b = new RowGroup.Builder(master.getRowSpacing());
				for (Block g : collection.getBlocks(id)) {
					AbstractBlockContentManager bcm = g.getBlockContentManager(c);
					b.addAll(bcm.getCollapsiblePreContentRows());
					b.addAll(bcm.getInnerPreContentRows());
					Optional<RowImpl> r;
					while ((r=bcm.getNext()).isPresent()) {
						b.add(r.get());
					}
					b.addAll(bcm.getPostContentRows());
					b.addAll(bcm.getSkippablePostContentRows());
				}
				return b.build();
			} else {
				return null;
			}
		}
		
	}
	
	private int calculateVerticalSpace(PageImpl pa, BlockPosition p, int blockSpace) {
		if (p != null) {
			int pos = p.getPosition().makeAbsolute(pa.getFlowHeight());
			int t = pos - pa.spaceUsedOnPage(0);
			if (t > 0) {
				int advance = 0;
				switch (p.getAlignment()) {
				case BEFORE:
					advance = t - blockSpace;
					break;
				case CENTER:
					advance = t - blockSpace / 2;
					break;
				case AFTER:
					advance = t;
					break;
				}
				return (int)Math.floor(advance / master.getRowSpacing());
			}
		}
		return 0;
	}
	
	public int getSizeLast() {
		if (master.duplex() && (size() % 2)==1) {
			return size() + 1;
		} else {
			return size();
		}
	}
	
	public int size() {
		return getToIndex()-fromIndex;
	}

	/**
	 * Gets the index for the first item in this sequence, counting all preceding items in the document, zero-based. 
	 * @return returns the first index
	 */
	public int getGlobalStartIndex() {
		return fromIndex;
	}

	public int getToIndex() {
		return toIndex;
	}

}
