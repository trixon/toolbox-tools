/*
 * Copyright 2019 Patrik Karlström.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.trixon.tools.mapollage;

import org.openide.util.lookup.ServiceProvider;
import se.trixon.toolbox.api.Tool;
import se.trixon.tools.mapollage.ui.MapollageModule;

/**
 *
 * @author Patrik Karlström
 */
@ServiceProvider(service = Tool.class)
public class MapollageTool extends Tool {

    public MapollageTool() {
        setName("Mapollage");
        setModule(new MapollageModule(getName()));
    }

}
