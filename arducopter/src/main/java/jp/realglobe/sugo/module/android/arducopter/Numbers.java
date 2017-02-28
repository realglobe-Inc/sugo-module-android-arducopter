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

/**
 * JSON の数値周りの変換関数
 * Created by fukuchidaisuke on 16/12/08.
 */
final class Numbers {

    private Numbers() {
    }

    /**
     * JSON 互換形式から変換する
     *
     * @param n JSON 互換の数値データ
     * @return double 値
     */
    static double decodeDouble(Object n) {
        if (n instanceof Double) {
            return (Double) n;
        } else if (n instanceof Float) {
            return ((Float) n).doubleValue();
        } else if (n instanceof Long) {
            return ((Long) n).doubleValue();
        } else if (n instanceof Integer) {
            return ((Integer) n).doubleValue();
        } else {
            throw new IllegalArgumentException("not a number");
        }
    }

    /**
     * JSON 互換形式から変換する
     *
     * @param n JSON 互換の数値データ
     * @return int 値
     */
    static int decodeInt(Object n) {
        if (n instanceof Double) {
            return ((Double) n).intValue();
        } else if (n instanceof Float) {
            return ((Float) n).intValue();
        } else if (n instanceof Long) {
            return ((Long) n).intValue();
        } else if (n instanceof Integer) {
            return (Integer) n;
        } else {
            throw new IllegalArgumentException("not a number");
        }
    }

}
