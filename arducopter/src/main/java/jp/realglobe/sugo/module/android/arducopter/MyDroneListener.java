/*----------------------------------------------------------------------
 * Copyright 2017 realglobe Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *----------------------------------------------------------------------*/

package jp.realglobe.sugo.module.android.arducopter;

import android.os.Bundle;
import android.util.Log;

import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import jp.realglobe.sugo.actor.Emitter;

/**
 * ドローンからのデータを受け取って中継する。
 * Created by fukuchidaisuke on 16/12/06.
 */
final class MyDroneListener implements DroneListener {

    private static final String LOG_TAG = MyDroneListener.class.getName();

    private final DroneWrapper drone;
    private final Emitter emitter;

    private final Set<Event> enableEvents;
    private Object lastPosition;

    MyDroneListener(DroneWrapper drone, Emitter emitter) {
        this.drone = drone;
        this.emitter = emitter;

        this.enableEvents = EnumSet.noneOf(Event.class);
        this.lastPosition = null;
    }

    @Override
    public void onDroneEvent(String event, Bundle extras) {
        switch (event) {
            case AttributeEvent.STATE_CONNECTED: {
                Log.d(LOG_TAG, "Drone connected");
                emit(Event.connected, null);
                break;
            }

            case AttributeEvent.STATE_DISCONNECTED: {
                Log.d(LOG_TAG, "Drone disconnected");
                emit(Event.disconnected, null);
                break;
            }

            case AttributeEvent.TYPE_UPDATED: {
                final Object data = this.drone.getType();
                Log.d(LOG_TAG, "Drone type updated: " + data);
                emit(Event.type, data);
                break;
            }

            case AttributeEvent.STATE_VEHICLE_MODE: {
                final Object data = this.drone.getMode();
                Log.d(LOG_TAG, "Drone mode updated: " + data);
                emit(Event.mode, data);
                break;
            }

            case AttributeEvent.STATE_ARMING: {
                if (this.drone.isArmed()) {
                    Log.d(LOG_TAG, "Drone armed");
                    emit(Event.armed, null);
                } else {
                    Log.d(LOG_TAG, "Drone disarmed");
                    emit(Event.disarmed, null);
                }
                break;
            }

            case AttributeEvent.SPEED_UPDATED: {
                final Object data = this.drone.getSpeed();
                Log.d(LOG_TAG, "Drone speed updated: " + data);
                emit(Event.speed, data);
                break;
            }

            case AttributeEvent.BATTERY_UPDATED: {
                final Object data = this.drone.getBattery();
                Log.d(LOG_TAG, "Drone battery updated: " + data);
                emit(Event.battery, data);
                break;
            }

            case AttributeEvent.HOME_UPDATED: {
                final Object data = this.drone.getHome();
                Log.d(LOG_TAG, "Drone home updated: " + data);
                emit(Event.home, data);
                break;
            }

            case AttributeEvent.ALTITUDE_UPDATED: {
                final Object data = this.drone.getPosition();
                if (data.equals(this.lastPosition)) {
                    return;
                }
                this.lastPosition = data;
                Log.d(LOG_TAG, "Drone altitude updated: " + data);
                emit(Event.position, data);
                break;
            }

            case AttributeEvent.GPS_POSITION: {
                final Object data = this.drone.getPosition();
                if (data.equals(this.lastPosition)) {
                    return;
                }
                this.lastPosition = data;
                Log.d(LOG_TAG, "Drone GPS position updated: " + data);
                emit(Event.position, data);
                break;
            }

            case AttributeEvent.ATTITUDE_UPDATED: {
                final Object data = this.drone.getAttitude();
                Log.d(LOG_TAG, "Drone attitude updated: " + data);
                emit(Event.attitude, data);
                break;
            }

            case AttributeEvent.MISSION_RECEIVED: {
                final Object data = this.drone.getMission();
                Log.d(LOG_TAG, "Drone mission received: " + data);
                emit(Event.mission, data);
                break;
            }

            case AttributeEvent.MISSION_SENT: {
                Log.d(LOG_TAG, "Drone mission saved");
                emit(Event.missionSaved, null);
                break;
            }

            case AttributeEvent.MISSION_ITEM_REACHED: {
                final Object data = this.drone.getReachedCommand();
                Log.d(LOG_TAG, "Drone mission command reached: " + data);
                emit(Event.commandReached, data);
                break;
            }

            case AttributeEvent.GIMBAL_ORIENTATION_UPDATED: {
                final Object data = this.drone.getGimbalOrientation();
                Log.d(LOG_TAG, "Drone gimbal orientation updated: " + data);
                emit(Event.gimbalOrientation, data);
            }

            default: {
                Log.d(LOG_TAG, "Drone event: " + event);
                break;
            }
        }
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {
        Log.d(LOG_TAG, "Drone interrupted");
    }

    private synchronized void emit(Event event, Object data) {
        if (this.enableEvents.contains(event)) {
            this.emitter.emit(event.name(), data);
        }
    }

    synchronized void enableEvents(Collection<Event> events) {
        this.enableEvents.addAll(events);
    }

    synchronized void disableEvents(Collection<Event> events) {
        this.enableEvents.removeAll(events);
    }

    synchronized List<Event> getEnableEvents() {
        return new ArrayList<>(enableEvents);
    }

}
