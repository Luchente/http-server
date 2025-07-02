package ru.netology;

public class Part {
    private final String name;
    private final String fileName;
    private final byte[] content;
    private final boolean isFile;
    private final String contentType;

    public Part(String name, String fileName, byte[] content, boolean isFile, String contentType) {
        this.name = name;
        this.fileName = fileName;
        this.content = content;
        this.isFile = isFile;
        this.contentType = contentType;
    }

    public String getName() {
        return name;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getContent() {
        return content;
    }

    public boolean isFile() {
        return isFile;
    }

    public String getContentType() {
        return contentType;
    }

    public String getContentAsString() {
        return new String(content);
    }
}
