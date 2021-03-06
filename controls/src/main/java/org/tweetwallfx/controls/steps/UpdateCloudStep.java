/*
 * The MIT License
 *
 * Copyright 2014-2015 TweetWallFX
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.tweetwallfx.controls.steps;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Transition;
import javafx.geometry.Bounds;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.tweetwallfx.controls.Word;
import org.tweetwallfx.controls.WordleLayout;
import org.tweetwallfx.controls.WordleSkin;
import org.tweetwallfx.controls.dataprovider.TagCloudDataProvider;
import org.tweetwallfx.controls.stepengine.AbstractStep;
import org.tweetwallfx.controls.stepengine.StepEngine.MachineContext;
import org.tweetwallfx.controls.transition.LocationTransition;

/**
 *
 * @author Jörg Michelberger
 */
public class UpdateCloudStep extends AbstractStep {
    
    private static final Logger log = LogManager.getLogger(UpdateCloudStep.class);            

    @Override
    public long preferredStepDuration(MachineContext context) {
        return 5000;
    }

    @Override
    public void doStep(MachineContext context) {
//        pane.setStyle("-fx-border-width: 1px; -fx-border-color: red;");
        WordleSkin wordleSkin = (WordleSkin)context.get("WordleSkin");
//        Wordle wordle = (Wordle)context.get("Wordle");

        List<Word> sortedWords = new ArrayList<>(wordleSkin.getSkinnable().wordsProperty().getValue());
        if (sortedWords.isEmpty()) {
            return;
        }

        Bounds layoutBounds = wordleSkin.getPane().getLayoutBounds();
        List<Word> limitedWords = sortedWords.stream().limit(wordleSkin.getDisplayCloudTags()).collect(Collectors.toList());
        List<Word> additionalTagCloudWords = wordleSkin.getSkinnable().getDataProvider(TagCloudDataProvider.class).getAdditionalTweetWords();
        
        double minWeight = limitedWords.stream().map(Word::getWeight).min(Comparator.naturalOrder()).get();
        
        if (null != additionalTagCloudWords) {
            additionalTagCloudWords.stream().map(word -> new Word(word.getText(), minWeight)).forEach(word -> limitedWords.add(word));
        }
        limitedWords.sort(Comparator.reverseOrder());

        WordleLayout.Configuration configuration = new WordleLayout.Configuration(limitedWords, wordleSkin.getFont(), wordleSkin.getFontSizeMin(), wordleSkin.getFontSizeMax(), layoutBounds);
        if (null != wordleSkin.getLogo()) {
            configuration.setBlockedAreaBounds(wordleSkin.getLogo().getBoundsInParent());
        }
        
        WordleLayout cloudWordleLayout = WordleLayout.createWordleLayout(configuration);
        List<Word> unusedWords = wordleSkin.word2TextMap.keySet().stream().filter(word -> !cloudWordleLayout.getWordLayoutInfo().containsKey(word)).collect(Collectors.toList());
        
        Duration defaultDuration = Duration.seconds(1.5);

        SequentialTransition morph = new SequentialTransition();

        List<Transition> fadeOutTransitions = new ArrayList<>();
        List<Transition> moveTransitions = new ArrayList<>();
        List<Transition> fadeInTransitions = new ArrayList<>();
        
        log.info("Unused words in cloud: " + unusedWords.stream().map(Word::getText).collect(Collectors.joining(", ")));        
        
        unusedWords.forEach(word -> {
            Text textNode = wordleSkin.word2TextMap.remove(word);

            FadeTransition ft = new FadeTransition(defaultDuration, textNode);
            ft.setToValue(0);
            ft.setOnFinished((event) -> {
                wordleSkin.getPane().getChildren().remove(textNode);
            });
            fadeOutTransitions.add(ft);
        });

        ParallelTransition fadeOuts = new ParallelTransition();
        fadeOuts.getChildren().addAll(fadeOutTransitions);
        morph.getChildren().add(fadeOuts);

        List<Word> existingWords = cloudWordleLayout.getWordLayoutInfo().keySet().stream().filter(word -> wordleSkin.word2TextMap.containsKey(word)).collect(Collectors.toList());

        log.info("Existing words in cloud: " + existingWords.stream().map(Word::getText).collect(Collectors.joining(", ")));
        existingWords.forEach(word -> {

            Text textNode = wordleSkin.word2TextMap.get(word);
            cloudWordleLayout.fontSizeAdaption(textNode, word.getWeight());
            Bounds bounds = cloudWordleLayout.getWordLayoutInfo().get(word);

            LocationTransition lt = new LocationTransition(defaultDuration, textNode);
            lt.setFromX(textNode.getLayoutX());
            lt.setFromY(textNode.getLayoutY());
            lt.setToX(bounds.getMinX() + layoutBounds.getWidth() / 2d);
            lt.setToY(bounds.getMinY() + layoutBounds.getHeight() / 2d + bounds.getHeight() / 2d);
            moveTransitions.add(lt);
        });

        ParallelTransition moves = new ParallelTransition();
        moves.getChildren().addAll(moveTransitions);
        morph.getChildren().add(moves);

        List<Word> newWords = cloudWordleLayout.getWordLayoutInfo().keySet().stream().filter(word -> !wordleSkin.word2TextMap.containsKey(word)).collect(Collectors.toList());

        List<Text> newTextNodes = new ArrayList<>();
        log.info("New words in cloud: " + newWords.stream().map(Word::getText).collect(Collectors.joining(", ")));
        newWords.forEach(word -> {
            Text textNode = cloudWordleLayout.createTextNode(word);
            wordleSkin.word2TextMap.put(word, textNode);

            Bounds bounds = cloudWordleLayout.getWordLayoutInfo().get(word);
            textNode.setLayoutX(bounds.getMinX() + layoutBounds.getWidth() / 2d);
            textNode.setLayoutY(bounds.getMinY() + layoutBounds.getHeight() / 2d + bounds.getHeight() / 2d);
            textNode.setOpacity(0);
            newTextNodes.add(textNode);
            FadeTransition ft = new FadeTransition(defaultDuration, textNode);
            ft.setToValue(1);
            fadeInTransitions.add(ft);
        });
        wordleSkin.getPane().getChildren().addAll(newTextNodes);
        
        ParallelTransition fadeIns = new ParallelTransition();
        fadeIns.getChildren().addAll(fadeInTransitions);
        morph.getChildren().add(fadeIns);
        
        morph.setOnFinished(e -> context.proceed());        
        morph.play();
    }
}
