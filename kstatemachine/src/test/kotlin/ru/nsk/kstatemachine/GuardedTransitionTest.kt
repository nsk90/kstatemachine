package ru.nsk.kstatemachine

import io.mockk.Called
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.jupiter.api.Test

class GuardedTransitionTest {
    @Test
    fun guardedTransition() {
        val callbacks = mockkCallbacks()

        var value = "value1"

        val machine = createStateMachine {
            val second = state("second")

            initialState("first") {
                transition<SwitchEvent> {
                    guard = { value == "value2" }
                    targetState = second
                    callbacks.listen(this)
                }
            }
        }

        machine.processEvent(SwitchEvent)
        verify { callbacks wasNot Called }

        value = "value2"
        machine.processEvent(SwitchEvent)
        verify { callbacks.onTriggeredTransition(SwitchEvent) }
    }

    @Test
    fun guardedTransitionOnWithLateinitState() {
        val callbacks = mockkCallbacks()

        var value = "value1"

        val machine = createStateMachine {
            lateinit var second: State

            initialState("first") {
                transitionOn<SwitchEvent> {
                    guard = { value == "value2" }
                    targetState = { second }
                    callbacks.listen(this)
                }
            }

            second = state("second")
        }

        machine.processEvent(SwitchEvent)
        verify { callbacks wasNot Called }

        value = "value2"
        machine.processEvent(SwitchEvent)
        verify { callbacks.onTriggeredTransition(SwitchEvent) }
    }

    @Test
    fun guardedTransitionSameEvent() {
        val callbacks = mockkCallbacks()

        lateinit var state1: State
        lateinit var state2: State
        lateinit var state3: State

        val machine = createStateMachine {
            state1 = initialState("state1") {
                callbacks.listen(this)

                transitionOn<SwitchEvent> {
                    guard = { false }
                    targetState = { state2 }
                    callbacks.listen(this)
                }

                transitionOn<SwitchEvent> {
                    guard = { true }
                    targetState = { state3 }
                    callbacks.listen(this)
                }
            }

            state2 = state("state2") { callbacks.listen(this) }
            state3 = state("state3") { callbacks.listen(this) }
        }

        verifySequenceAndClear(callbacks) { callbacks.onEntryState(state1) }

        machine.processEvent(SwitchEvent)

        verifySequence {
            callbacks.onTriggeredTransition(SwitchEvent)
            callbacks.onExitState(state1)
            callbacks.onEntryState(state3)
        }
    }
}