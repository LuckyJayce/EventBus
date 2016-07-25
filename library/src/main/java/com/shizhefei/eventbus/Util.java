/**
 * Copyright 2015 shizhefei（LuckyJayce）
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shizhefei.eventbus;

import java.util.ArrayList;


/**
 * Created by LuckyJayce on 2016/7/23.
 */
class Util {

    @SuppressWarnings("unchecked")
    static ArrayList<Class<? extends IEvent>> getInterfaces(IEvent event) {
        Class<?>[] interfaces = event.getClass().getInterfaces();
        ArrayList<Class<? extends IEvent>> eventClass = new ArrayList<>();
        for (Class<?> in : interfaces) {
            if (isExtendsInterface(in, IEvent.class)) {
                eventClass.add((Class<? extends IEvent>) in);
            }
        }
        return eventClass;
    }


    static boolean isExtendsInterface(Class<?> in, Class<?> superClass) {
        Class<?>[] subIns = in.getInterfaces();
        for (Class<?> subIn : subIns) {
            if (IEvent.class.equals(subIn)) {
                return true;
            }
        }
        return false;
    }


    static <T> void validateServiceInterface(Class<T> service) {
        if (!service.isInterface()) {
            throw new IllegalArgumentException(
                    "API declarations must be interfaces.");
        }
        if (!isExtendsInterface(service, IEvent.class)) {
            throw new IllegalArgumentException(
                    "API declarations must be extends IEvent.");
        }

    }
}
