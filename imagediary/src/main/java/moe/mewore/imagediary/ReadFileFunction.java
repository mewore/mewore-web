package moe.mewore.imagediary;

import java.io.File;
import java.io.IOException;

@FunctionalInterface
public interface ReadFileFunction<T> {

    T apply(File file) throws IOException;
}
