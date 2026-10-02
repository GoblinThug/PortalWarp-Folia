package ru.goblin_thug.portalwarp.managers;

import ru.goblin_thug.portalwarp.utils.FrameData;

public class Portal {
    private final String name;
    private FrameData frame1;
    private FrameData frame2;
    private String serverAddress;

    public Portal(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    public FrameData getFrame1() { return frame1; }
    public void setFrame1(FrameData frame1) { this.frame1 = frame1; }

    public FrameData getFrame2() { return frame2; }
    public void setFrame2(FrameData frame2) { this.frame2 = frame2; }

    public String getServerAddress() { return serverAddress; }
    public void setServerAddress(String serverAddress) { this.serverAddress = serverAddress; }

    public boolean isCrossServer() { return serverAddress != null; }
}
