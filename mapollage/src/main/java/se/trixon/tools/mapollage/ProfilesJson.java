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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import org.apache.commons.io.FileUtils;
import se.trixon.tools.mapollage.profile.Profile;

/**
 *
 * @author Patrik Karlström
 */
public class ProfilesJson {

    private static final int FILE_FORMAT_VERSION = 2;
    private static final Gson GSON = new GsonBuilder()
            .setVersion(1.0)
            .serializeNulls()
            .setPrettyPrinting()
            .create();
    @SerializedName("format_version")
    private int mFileFormatVersion;
    @SerializedName("profiles")
    private final LinkedList<Profile> mProfiles = new LinkedList<>();

    public static ProfilesJson open(File file) throws IOException, JsonSyntaxException {
        String json = FileUtils.readFileToString(file, Charset.defaultCharset());
        ProfilesJson profiles = null;
        try {
            profiles = GSON.fromJson(json, ProfilesJson.class);
            if (profiles.mFileFormatVersion != FILE_FORMAT_VERSION) {
                //TODO Handle file format version change
            }
        } catch (JsonSyntaxException e) {
            System.err.println(e.getMessage());
        }

        return profiles;
    }

    public int getFileFormatVersion() {
        return mFileFormatVersion;
    }

    public LinkedList<Profile> getProfiles() {
        return mProfiles;
    }

    public void save(File file) throws IOException {
        mFileFormatVersion = FILE_FORMAT_VERSION;
        FileUtils.writeStringToFile(file, GSON.toJson(this), Charset.defaultCharset());
    }
}
