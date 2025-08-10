package com.mkso4ka.mindustry.matrixproc;

import mindustry.mod.*;
import mindustry.game.EventType.*;
import arc.util.Log;
import arc.Events; // <-- ИСПРАВЛЕНИЕ 3: Добавлен этот импорт

public class PictureToLogicMod extends Mod {

    public PictureToLogicMod(){
        // Теперь компилятор знает, что такое "Events"
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