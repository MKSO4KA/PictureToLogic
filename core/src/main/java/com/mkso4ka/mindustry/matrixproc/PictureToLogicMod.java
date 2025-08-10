package com.mkso4ka.mindustry.matrixproc;

import mindustry.mod.*;
import mindustry.game.EventType.*;
import arc.Events;

public class PictureToLogicMod extends Mod {

    public PictureToLogicMod(){
        // Используем наш новый логгер
        WebLogger.info("PictureToLogicMod constructor called.");
    }

    @Override
    public void init(){
        Events.on(ClientLoadEvent.class, e -> {
            // Запускаем наш веб-сервер
            WebLogger.startServer();
            
            ModUI.build();
            WebLogger.info("PictureToLogic UI Initialized.");
        });
    }

    @Override
    public void loadContent(){
        WebLogger.info("Loading PictureToLogic content...");
    }
}