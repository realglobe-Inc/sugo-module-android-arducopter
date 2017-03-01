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

import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.drone.mission.item.MissionItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ミッションについて。
 * Created by fukuchidaisuke on 16/12/05.
 */
final class Missions {


    private static final String KEY_TYPE = "type";

    private static final String COMMAND_TYPE_UNKNOWN = "unknown";

    private Missions() {
    }

    /**
     * JSON 互換形式に変換する
     *
     * @param mission ミッション
     * @return ミッションを表す JSON 互換データ
     */
    static List<Map<String, Object>> encode(Mission mission) {
        final List<Map<String, Object>> encodedCommands = new ArrayList<>();
        for (MissionItem command : mission.getMissionItems()) {
            encodedCommands.add(encode(command));
        }
        return encodedCommands;
    }

    /**
     * JSON 互換形式から変換する
     *
     * @param commands ミッションを表す JSON 互換データ
     * @return ミッション
     */
    static Mission decode(Object[] commands) {
        final Mission mission = new Mission();
        for (Object command : commands) {
            mission.addMissionItem(decodeCommand((Map<String, Object>) command));
        }
        return mission;
    }

    private static Map<String, Object> encode(MissionItem command) {
        final Command type = Command.correspondTo(command.getClass());
        final Map<String, Object> encoded;
        if (type == null) {
            encoded = new HashMap<>();
            encoded.put(KEY_TYPE, COMMAND_TYPE_UNKNOWN);
        } else {
            encoded = type.encode(command);
            encoded.put(KEY_TYPE, type.name());
        }
        return encoded;
    }

    private static MissionItem decodeCommand(Map<String, Object> command) {
        final Command type = Command.valueOf((String) command.get(KEY_TYPE));
        final Map<String, Object> copy = new HashMap<>(command);
        copy.remove(KEY_TYPE);

        final MissionItem decoded = type.decode(copy);

        if (!copy.isEmpty()) {
            throw new IllegalArgumentException("invalid parameters " + copy.keySet());
        }
        return decoded;
    }

}
