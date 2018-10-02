package de.unitrier.st.soposthistory.gt.ComparatorAppOfGroundTruthAndComputedSimilarity;

class BlockPair {

    PostBlockWebView leftBlock;
    PostBlockWebView rightBlock;
    int leftVersion;

    BlockPair(
            PostBlockWebView leftBlock,
            PostBlockWebView rightBlock,
            int leftVersion) {

        this.leftBlock = leftBlock;
        this.rightBlock = rightBlock;
        this.leftVersion = leftVersion;
    }


    @Override
    public boolean equals(Object blockPair){
        return (blockPair instanceof BlockPair) && ((BlockPair) blockPair).leftBlock.equals(this.rightBlock)
                && ((BlockPair) blockPair).leftBlock.equals(this.rightBlock)
                && ((BlockPair) blockPair).leftVersion == this.leftVersion;
    }
}
