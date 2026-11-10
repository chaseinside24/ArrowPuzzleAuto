package com.example.arrowpuzzleauto;

import java.util.Arrays;
import java.util.List;

public final class BoardRecognizer {
    public static final class Result {
        public final int[] states;
        public final int unknownTiles;
        public final boolean claimTextVisible;
        public final int geometryRotation;
        final int confidence;

        public Result(int[] states, int unknownTiles, boolean claimTextVisible,
                      int geometryRotation, int confidence) {
            this.states = states;
            this.unknownTiles = unknownTiles;
            this.claimTextVisible = claimTextVisible;
            this.geometryRotation = geometryRotation;
            this.confidence = confidence;
        }

        public boolean isSolved() {
            if (unknownTiles != 0) return false;
            for (int state : states) if (state != 1) return false;
            return true;
        }
    }

    private static final int[] ANGLES = {0,45,90,135,180,225,270,315};
    private static final float CLAIM_LABEL_MIN_SPAN = 0.33f;

    public Result read(ScreenFrame frame) {
        if (frame == null || frame.width < 300 || frame.height < 600) return null;
        float aspect = (float) frame.width / frame.height;
        if (!ArrowPuzzleGeometry.isSupportedAspect(aspect)) return null;

        // Landscape can be mirrored in either direction. Both are checked;
        // claim-label visibility resolves the direction once the board is solved.
        int[] rotations = frame.width >= frame.height ? new int[]{1,3} : new int[]{0};
        Result best = null;
        for (int rotation : rotations) {
            Result candidate = readWithRotation(frame, rotation);
            if (best == null
                    || (candidate.claimTextVisible && !best.claimTextVisible)
                    || (candidate.claimTextVisible == best.claimTextVisible
                        && candidate.unknownTiles < best.unknownTiles)
                    || (candidate.claimTextVisible == best.claimTextVisible
                        && candidate.unknownTiles == best.unknownTiles
                        && candidate.confidence > best.confidence)) {
                best = candidate;
            }
        }
        return best;
    }

    private Result readWithRotation(ScreenFrame frame, int rotation) {
        List<ArrowPuzzleGeometry.PointN> centers =
                ArrowPuzzleGeometry.centers(frame.width, frame.height, rotation);
        int radius = ArrowPuzzleGeometry.sampleRadiusPx(frame.width, frame.height);
        int[] states = new int[centers.size()];
        int unknown = 0, confidence = 0;

        for (int i = 0; i < centers.size(); i++) {
            ArrowPuzzleGeometry.PointN p = centers.get(i);
            int cx = Math.round(p.x * frame.width);
            int cy = Math.round(p.y * frame.height);
            int[] samples = new int[ANGLES.length];
            for (int j = 0; j < ANGLES.length; j++) {
                double a = Math.toRadians(ANGLES[j]);
                samples[j] = frame.luminance(
                        cx + Math.round((float) (radius * Math.cos(a))),
                        cy + Math.round((float) (radius * Math.sin(a))));
            }
            Arrays.sort(samples);
            int median = (samples[3] + samples[4]) / 2;
            if (median < 48) {
                states[i] = 1;
                confidence += 48 - median;
            } else if (median > 65) {
                states[i] = 2;
                confidence += median - 65;
            } else {
                states[i] = 0;
                unknown++;
                confidence -= 100;
            }
        }
        return new Result(states, unknown, isClaimTextVisible(frame, rotation),
                rotation, confidence);
    }

    private boolean isClaimTextVisible(ScreenFrame frame, int rotation) {
        boolean tablet = ArrowPuzzleGeometry.isTabletProfile(
                (float) frame.width / frame.height);
        float x0 = 0.20f, x1 = 0.75f;
        float y0 = tablet ? 0.895f : 0.86f;
        float y1 = tablet ? 0.925f : 0.90f;

        ArrowPuzzleGeometry.PointN[] corners = {
            ArrowPuzzleGeometry.orient(new ArrowPuzzleGeometry.PointN(x0,y0), rotation),
            ArrowPuzzleGeometry.orient(new ArrowPuzzleGeometry.PointN(x0,y1), rotation),
            ArrowPuzzleGeometry.orient(new ArrowPuzzleGeometry.PointN(x1,y0), rotation),
            ArrowPuzzleGeometry.orient(new ArrowPuzzleGeometry.PointN(x1,y1), rotation)
        };
        float minX = 1f, minY = 1f, maxX = 0f, maxY = 0f;
        for (ArrowPuzzleGeometry.PointN p : corners) {
            minX = Math.min(minX,p.x); minY = Math.min(minY,p.y);
            maxX = Math.max(maxX,p.x); maxY = Math.max(maxY,p.y);
        }
        int left = Math.max(0, Math.round(minX * frame.width));
        int right = Math.min(frame.width, Math.round(maxX * frame.width));
        int top = Math.max(0, Math.round(minY * frame.height));
        int bottom = Math.min(frame.height, Math.round(maxY * frame.height));
        int minBrightX = right, maxBrightX = -1, minBrightY = bottom, maxBrightY = -1;

        for (int y = top; y < bottom; y++) {
            for (int x = left; x < right; x++) {
                if (frame.luminance(x,y) >= 185) {
                    minBrightX = Math.min(minBrightX,x);
                    maxBrightX = Math.max(maxBrightX,x);
                    minBrightY = Math.min(minBrightY,y);
                    maxBrightY = Math.max(maxBrightY,y);
                }
            }
        }
        if (maxBrightX < minBrightX) return false;
        int span = (rotation == 1 || rotation == 3)
                ? maxBrightY - minBrightY + 1
                : maxBrightX - minBrightX + 1;
        return span >= Math.min(frame.width, frame.height) * CLAIM_LABEL_MIN_SPAN;
    }
}
