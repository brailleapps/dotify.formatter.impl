package org.daisy.dotify.formatter.impl.sheet;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * {@link VolumeSplitter} implementation that gives preference to even sized volumes.
 *
 * <p>Each {@link SheetGroup volume group} has its own <code>VolumeSplitter</code> instance.</p>
 *
 * <p>The target sizes of the volumes are computed in {@link EvenSizeVolumeSplitterCalculator} based
 * on a total number of sheets and a <code>volumeOffset</code> parameter. By increasing this
 * parameter, extra volumes are added on top of the number strictly needed to accommodate all the
 * sheets, and the volume sizes are decreased. Different configurations are tried and evaluated
 * based on information about the previous iterations, provided through {@link
 * #updateSheetCount(int, int) updateSheetCount}:</p>
 *
 * <ul>
 *   <li>The actual total number of sheets in the volume group. "Total" means including remaining
 *       sheets and sheets coming from the pre- and post-content.</li>
 *   <li>The number of sheets that did not fit in the volume group.</li>
 * </ul>
 *
 * <p>The actual sizes of the volumes are not determined here. This is done in {@link
 * org.daisy.dotify.formatter.impl.VolumeProvider}.</p>
 */
class EvenSizeVolumeSplitter implements VolumeSplitter {
    /*
    Q: Is this the only class that implements VolumeSplitter?
    A (Bert): yes
    Q: Suppose I just want to fill the first volume, split it at a good place, go to the next volume, etc,
        and I don't care about the size of the last volume.
        Would this be a much easier approach?
    A (Bert): It would simplify things a bit, but not that much I think.
    */
	private static final Logger logger = Logger.getLogger(EvenSizeVolumeSplitter.class.getCanonicalName());
	private EvenSizeVolumeSplitterCalculator sdc;
	private final SplitterLimit splitterMax;
	// number of volumes to add on top of the number of volumes strictly needed to accommodate the
	// total number of sheets
	int volumeOffset = 0;
	
	/*
	 * This map keeps track of which split suggestions resulted in a successful split. We
	 * make use of this information in order to not get into an endless loop while looking
	 * for the optimal number of volumes.
	 * (Paul) This is the only place where information from all previous iterations is used.
	 *	At other places, e.g. the LookupHandlers, only information from the previous iteration is used,
	 *	for comparison purposes (setting of the dirty flag).
	 */
	private Map<EvenSizeVolumeSplitterCalculator,Boolean> previouslyTried = new HashMap<>();
	
	EvenSizeVolumeSplitter(SplitterLimit splitterMax) {
		this.splitterMax = splitterMax;
	}
	
	/**
	 * Provide information about the actual volumes in the previous iteration.
	 *
	 * @param sheets the total number of sheets in the volume group, including remaining sheets and
	 *     sheets coming from pre- or post-content (overhead)
	 * @param remainingSheets the number of sheets that did not fit in the volume group
	 */
	@Override
	public void updateSheetCount(int sheets, int remainingSheets) {
		if (sdc == null) {
			sdc = new EvenSizeVolumeSplitterCalculator(sheets, splitterMax, volumeOffset);
		} else {
			boolean sheetsFitInVolumes = remainingSheets == 0;
			EvenSizeVolumeSplitterCalculator prvSdc = sdc;
			sdc = null;
			previouslyTried.put(prvSdc, sheetsFitInVolumes);
			if (!sheetsFitInVolumes) {
				EvenSizeVolumeSplitterCalculator esc = new EvenSizeVolumeSplitterCalculator(sheets, splitterMax, volumeOffset);
				
				// Try increasing the volume count
				/*
				Q: Why would you ever want to do this?
				A: The right question is: "why _wouldn't_ you want to do this? It is possible that this solution
					hasn't been tried before, and we might have added too many volumes in the previous iteration.
				*/
				if (remainingSheets >= sheets) {
					throw new IllegalStateException();
				}
				int volumeInc; {
					double inc = (1.0 * prvSdc.getVolumeCount() * remainingSheets) / (sheets - remainingSheets);
					// subtract increase in volume count due to increase in sheet count
					inc -= (esc.getVolumeCount() - prvSdc.getVolumeCount());
					// factor 3/4 because we don't want to adapt too fast
					inc *= .75;
					volumeInc = (int)Math.floor(inc);
				}
				if (volumeInc > 0) {
					volumeOffset += volumeInc;
					sdc = new EvenSizeVolumeSplitterCalculator(sheets, splitterMax, volumeOffset);
				} else {
					
					// Try with adjusted number of sheets
					if (!previouslyTried.containsKey(esc) || previouslyTried.get(esc)) {
						sdc = esc;
					} else {
						volumeOffset += 1;
						sdc = new EvenSizeVolumeSplitterCalculator(sheets, splitterMax, volumeOffset);
					}
				}
			} else {
				if (volumeOffset > 0) {
					
					// Try decreasing the volume count again
					EvenSizeVolumeSplitterCalculator esc = new EvenSizeVolumeSplitterCalculator(sheets, splitterMax, volumeOffset - 1);
					if (!previouslyTried.containsKey(esc)) {
						volumeOffset--;
						sdc = esc;
					}
				}
				if (sdc == null) {
					
					// Try with up to date sheet count
					EvenSizeVolumeSplitterCalculator esc = new EvenSizeVolumeSplitterCalculator(sheets, splitterMax, volumeOffset);
					if (!previouslyTried.containsKey(esc)) {
						sdc = esc;
					} else {
						sdc = prvSdc;
					}
				}
			}
		}
	}

	@Override
	public int sheetsInVolume(int volIndex) {
		return sdc.sheetsInVolume(volIndex);
	}

	@Override
	public int getVolumeCount() {
		return sdc.getVolumeCount();
	}
}
