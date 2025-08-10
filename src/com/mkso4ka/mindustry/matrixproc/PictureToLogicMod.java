// src/com/mkso4ka/mindustry/matrixproc/PictureToLogicMod.java
package com.mkso4ka.mindustry.matrixproc;

import mindustry.mod.*;
import mindustry.game.EventType.*;
import arc.util.Log;
import arc.Events;

public class PictureToLogicMod extends Mod {

    public PictureToLogicMod(){
        // Этот конструктор вызывается при обнаружении мода.
        // Оставляем его для логов или предварительных действий.
        Log.info("PictureToLogicMod constructor called.");
    }

    @Override
    public void init(){
        // Метод init() вызывается после загрузки всех основных компонентов игры.
        // Это самое правильное и безопасное место для создания и добавления UI элементов.
        Events.on(ClientLoadEvent.class, e -> {
            // Запускаем построение нашего UI
            ModUI.build();
            Log.info("PictureToLogic UI Initialized.");
        });
    }

    @Override
    public void loadContent(){
        Log.info("Loading PictureToLogic content...");
        // Этот метод предназначен для загрузки контента (блоков, предметов и т.д.),
        // для UI он не совсем подходит.
    }
}