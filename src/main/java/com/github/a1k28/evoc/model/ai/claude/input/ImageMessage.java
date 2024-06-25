package com.github.a1k28.evoc.model.ai.claude.input;

import com.github.a1k28.evoc.model.ai.claude.types.ImageType;
import com.google.gson.JsonObject;
import lombok.Getter;

@Getter
public class ImageMessage implements ClaudeMessage {
    private final String base64;
    private final String mediaType;
    public ImageMessage(String base64, String mediaType) {
        this.base64 = base64;
        this.mediaType = "image/"+mediaType;
        if (!ImageType.JPEG.value().equalsIgnoreCase(mediaType) && !ImageType.PNG.value().equalsIgnoreCase(mediaType))
            throw new RuntimeException("Invalid image type: " + mediaType);
    }

    @Override
    public JsonObject toJsonObject() {
        JsonObject image = new JsonObject();
        image.addProperty("type", "image");
        JsonObject source = new JsonObject();
        source.addProperty("type", "base64");
        source.addProperty("media_type", mediaType);
        source.addProperty("data", base64);
        image.add("source", source);
        return image;
    }
}
