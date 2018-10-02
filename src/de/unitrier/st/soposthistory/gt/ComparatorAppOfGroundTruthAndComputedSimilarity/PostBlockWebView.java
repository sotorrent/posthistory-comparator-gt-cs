package de.unitrier.st.soposthistory.gt.ComparatorAppOfGroundTruthAndComputedSimilarity;

import org.sotorrent.posthistoryextractor.blocks.PostBlockVersion;

class PostBlockWebView {

    WebViewFitContent webViewFitContent = new WebViewFitContent("");
    PostBlockVersion postBlock;

    PostBlockWebView(PostBlockVersion postBlockVersion) {
        this.postBlock = postBlockVersion;
    }
}

