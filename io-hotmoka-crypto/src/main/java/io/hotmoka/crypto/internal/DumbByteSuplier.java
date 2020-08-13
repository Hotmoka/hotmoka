/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hotmoka.crypto.internal;

import io.hotmoka.crypto.BytesSupplier;
import java.nio.charset.StandardCharsets;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.util.encoders.UTF8;

/**
 *
 * @author giovanni.antino@h2tcoin.com
 */
public class DumbByteSuplier implements BytesSupplier<Object> {

    @Override
    public byte[] get(Object what) throws Exception {
        if (what instanceof String) {
            String s = (String) what;
            return s.getBytes(StandardCharsets.UTF_8);
        } else if (what instanceof byte[]) {
            return (byte[]) what;
        } else {
            return null;
        }
    }

}
