package com.tvd12.ezyhttp.core.codec;

import java.io.IOException;

public interface BodySerializer {

    byte[] serialize(Object body) throws IOException;
    
}
