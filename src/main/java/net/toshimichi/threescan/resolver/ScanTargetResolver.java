package net.toshimichi.threescan.resolver;

import net.toshimichi.threescan.scanner.ScanTarget;

import java.io.IOException;

public interface ScanTargetResolver {

    ScanTarget next() throws IOException;
}
