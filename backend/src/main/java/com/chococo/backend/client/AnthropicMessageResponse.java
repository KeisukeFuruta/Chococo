package com.chococo.backend.client;

import java.util.List;

// レスポンスのうち、実際に使うcontent[].textのみを受け取る最小限のマッピング
public record AnthropicMessageResponse(List<ContentBlock> content) {

    public record ContentBlock(String type, String text) {
    }
}
