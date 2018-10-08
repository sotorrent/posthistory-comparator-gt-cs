package de.unitrier.st.soposthistory.gt.ComparatorAppOfGroundTruthAndComputedSimilarity;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Worker;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSException;

import java.util.Set;

// http://tech.chitgoks.com/2014/09/13/how-to-fit-webview-height-based-on-its-content-in-java-fx-2-2/
public final class WebViewFitContent extends Region {

    WebView webview = new WebView();
    WebEngine webEngine = webview.getEngine();

    WebViewFitContent(String content) {
        webview.setPrefHeight(5);

        widthProperty().addListener((ChangeListener<Object>) (observable, oldValue, newValue) -> {
            Double width = (Double)newValue;
            webview.setPrefWidth(width);
            adjustHeight();
        });

        webview.getEngine().getLoadWorker().stateProperty().addListener((arg0, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                adjustHeight();
            }
        });

        webview.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
            Set<Node> scrolls = webview.lookupAll(".scroll-bar");
            for (Node scroll : scrolls) {
                scroll.setVisible(false);
            }
        });

        setContent(content);
        getChildren().add(webview);
    }

    public void setContent(final String content) {
        Platform.runLater(() -> {
            webEngine.loadContent(getHtml(content));
            Platform.runLater(this::adjustHeight);
        });
    }


    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        layoutInArea(webview,0,0,w,h,0, HPos.CENTER, VPos.CENTER);
    }

    private void adjustHeight() {
        Platform.runLater(() -> {
            try {
                Object result = webEngine.executeScript("document.getElementById('mydiv').offsetHeight");
                if (result instanceof Integer) {
                    double height = (Integer) result;
                    height = height + 30;
                    webview.setPrefHeight(height);
                    webview.getPrefHeight();
                }
            } catch (JSException ignored) { }
        });
    }

    private String getHtml(String content) {
        return "<html><body>" +
                "<div id=\"mydiv\">" + content + "</div>" +
                "</body></html>";
    }

}
