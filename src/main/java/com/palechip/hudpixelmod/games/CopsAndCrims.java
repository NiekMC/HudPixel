package com.palechip.hudpixelmod.games;

import com.palechip.hudpixelmod.HudPixelConfig;
import com.palechip.hudpixelmod.components.CoinCounterComponent;
import com.palechip.hudpixelmod.components.TimerComponent;

import net.hypixel.api.util.GameType;

public class CopsAndCrims extends Game {

    public CopsAndCrims() {
        super("", "Cops and Crims", START_MESSAGE_DEFAULT, END_MESSAGE_DEFAULT, GameType.MCGO, HudPixelConfig.COPS_AND_CRIMS_CATEGORY);
        if(HudPixelConfig.copsAndCrimsTimeDisplay) {
            this.components.add(new TimerComponent());   
        }
        if(HudPixelConfig.copsAndCrimsCoinDisplay) {
            this.components.add(new CoinCounterComponent());
        }
    }

}
