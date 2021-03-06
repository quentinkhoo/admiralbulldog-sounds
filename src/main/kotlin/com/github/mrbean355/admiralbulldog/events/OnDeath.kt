package com.github.mrbean355.admiralbulldog.events

import com.github.mrbean355.admiralbulldog.game.GameState

class OnDeath : RandomSoundEvent {
    override val chance = 0.33f

    override fun shouldPlay(previous: GameState, current: GameState): Boolean {
        return current.player!!.deaths > previous.player!!.deaths
    }
}