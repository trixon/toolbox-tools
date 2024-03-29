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
package se.trixon.tools.fbd;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Patrik Karlström
 */
public class FileVisitor extends SimpleFileVisitor<Path> {

    private List<File> mFiles = new ArrayList<>();
    private boolean mInterrupted;
    private final Operation mOperation;
    private final OperationListener mOperationListener;
    private final PathMatcher mPathMatcher;

    public FileVisitor(PathMatcher pathMatcher, List<File> paths, Operation operation) {
        mFiles = paths;
        mPathMatcher = pathMatcher;
        mOperation = operation;
        mOperationListener = operation.getListener();
    }

    public boolean isInterrupted() {
        return mInterrupted;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        try {
            TimeUnit.NANOSECONDS.sleep(1);
        } catch (InterruptedException ex) {
            mInterrupted = true;
            return FileVisitResult.TERMINATE;
        }

        mOperationListener.onOperationLog(dir.toString());
        String[] filePaths = dir.toFile().list();

        if (filePaths != null && filePaths.length > 0) {
            for (String fileName : filePaths) {
                try {
                    TimeUnit.NANOSECONDS.sleep(1);
                } catch (InterruptedException ex) {
                    mInterrupted = true;
                    return FileVisitResult.TERMINATE;
                }
                File file = new File(dir.toFile(), fileName);
                if (file.isFile() && mPathMatcher.matches(file.toPath().getFileName())) {
                    mFiles.add(file);
                }
            }
        }

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        return FileVisitResult.CONTINUE;
    }
}
