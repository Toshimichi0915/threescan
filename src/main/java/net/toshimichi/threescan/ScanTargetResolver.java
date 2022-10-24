package net.toshimichi.threescan;

import java.io.IOException;

public interface ScanTargetResolver {

    ScanTarget next() throws IOException;
}
