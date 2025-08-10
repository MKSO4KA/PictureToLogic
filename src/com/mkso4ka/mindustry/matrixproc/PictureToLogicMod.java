package com.mkso4ka.mindustry.matrixproc;

import mindustry.mod.*;
import mindustry.game.EventType.*;
import arc.util.Log;
import arc.Events;

public class PictureToLogicMod extends Mod {

    public PictureToLogicMod(){
        Events.on(ClientLoadEvent.class, e -> {
            ModUI.build();
            Log.info("PictureToLogic UI Initialized.");
        });
    }

    @Override
    public void loadContent(){
        Log.info("Loading PictureToLogic content...");
    }
}