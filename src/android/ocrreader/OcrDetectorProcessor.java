/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thecodealer.OcrTimeFromCamera.ocrreader;

import android.util.Log;
import android.util.SparseArray;

import com.thecodealer.OcrTimeFromCamera.ocrreader.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * A very simple Processor which gets detected TextBlocks and adds them to the overlay
 * as OcrGraphics.
 */
public class OcrDetectorProcessor implements Detector.Processor<TextBlock> {

    private GraphicOverlay<OcrGraphic> graphicOverlay;
    private OcrCaptureActivity captureActivity;
    private Map<String, Integer> matchedTimePatterns = new HashMap<>();

    OcrDetectorProcessor(GraphicOverlay<OcrGraphic> ocrGraphicOverlay, OcrCaptureActivity activity) {
        graphicOverlay = ocrGraphicOverlay;
        captureActivity = activity;
    }

    /**
     * Called by the detector to deliver detection results.
     * If your application called for it, this could be a place to check for
     * equivalent detections by tracking TextBlocks that are similar in location and content from
     * previous frames, or reduce noise by eliminating TextBlocks that have not persisted through
     * multiple detections.
     */
    @Override
    public void receiveDetections(Detector.Detections<TextBlock> detections) {
        graphicOverlay.clear();
        SparseArray<TextBlock> items = detections.getDetectedItems();
        for (int i = 0; i < items.size(); ++i) {
            TextBlock item = items.valueAt(i);
            if (item != null && item.getValue() != null) {
                if (this.isTime(item.getValue())) {
                    OcrGraphic graphic = new OcrGraphic(graphicOverlay, item);
                    graphicOverlay.add(graphic);
                    // Pattern p = Pattern.compile("\\d{1,3}(:|\\s|\\.)\\d{2}");
                    Pattern p = Pattern.compile("\\d{2,3}(:|\\.)\\d{2}");
                    Matcher m = p.matcher(item.getValue());
                    if (m.find()) {
                        String formattedText = m.group(0).trim().replaceAll("(\\s|\\.)+", ":");
                        String formattedTextPattern = formattedText.substring(0, formattedText.length() - 1);
                        if (this.isBestMatch(formattedTextPattern)) {
                            captureActivity.onTimeMatched(formattedText);
                            matchedTimePatterns.clear();
                            release();
                        }

                        if (matchedTimePatterns.containsKey(formattedTextPattern)) {
                            matchedTimePatterns.put(formattedTextPattern, matchedTimePatterns.get(formattedTextPattern) + 1);
                        }
                        else {
                            matchedTimePatterns.put(formattedTextPattern, 1);
                        }
                    }
                }
            }
        }
    }

    /**
     * Frees the resources associated with this detection processor.
     */
    @Override
    public void release() {
        graphicOverlay.clear();
    }

    private boolean isTime(String text) {
        // String timeRegexString = ".*(^|\\s)\\d{1,3}(:|\\s|\\.)\\d{2}($|\\s).*";
        String timeRegexString = ".*(^|\\s)\\d{2,3}(:|\\.)\\d{2}($|\\s).*";
        return Pattern.matches(timeRegexString, text);
    }

    private boolean isBestMatch(String pattern) {
        boolean output = false;
        if (matchedTimePatterns.containsKey(pattern)) {
            output = matchedTimePatterns.get(pattern) > 2;
        }
        return output;
    }
}
