// src/main/java/com/mkso4ka/mindustry/matrixproc/PictureToLogicMod.java

package com.mkso4ka.mindustry.matrixproc;

import mindustry.mod.*;
import mindustry.game.EventType.*;
import arc.util.Log;

public class PictureToLogicMod extends Mod {

    public PictureToLogicMod(){
        // Мы будем слушать событие загрузки клиента
        Events.on(ClientLoadEvent.class, e -> {
            // Когда игра загрузится, вызываем наш метод для построения UI
            ModUI.build();
            Log.info("PictureToLogic UI Initialized.");
        });
    }

    @Override
    public void loadContent(){
        Log.info("Loading PictureToLogic content...");
    }
}