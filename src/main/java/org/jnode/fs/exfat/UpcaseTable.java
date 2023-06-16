/*
 * $Id$
 *
 * Copyright (C) 2003-2015 JNode.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; If not, write to the Free Software Foundation, Inc., 
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
 
package org.jnode.fs.exfat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * TODO: Add support for compressed format:
 * http://www.ntfs.com/exfat-upcase-table.htm
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public final class UpcaseTable {

    public static UpcaseTable read(ExFatSuperBlock sb,
                                   long startCluster, long size, long checksum) throws IOException {

        Cluster.checkValid(startCluster);
        
        /* validate size */

        if ((size == 0) || (size > (0xffff * 2)) || (size % 2) != 0) {
            throw new IOException("bad upcase table size " + size);
        }

        final UpcaseTable result = new UpcaseTable(sb,
            sb.clusterToOffset(startCluster), size);
        
        /* verify checksum */
        final long actualCs = result.checkSum();
        if (checksum != actualCs) {

            final StringBuilder msg = new StringBuilder();
            msg.append("checksum mismatch (expected 0x");
            msg.append(Long.toHexString(checksum));
            msg.append(", got 0x");
            msg.append(Long.toHexString(actualCs));
            msg.append(")");

            throw new IOException(msg.toString());
        }

        return result;
    }

    private final ExFatSuperBlock sb;

    /**
     * Size in bytes.
     */
    private final long size;

    /**
     * Number of chars.
     */
    private final long chars;

    /**
     * Offset to the first byte of the table, relative to byte 0 on the
     * device.
     */
    private final long offset;

    private final DeviceAccess da;

    ByteBuffer buffer;

    private UpcaseTable(ExFatSuperBlock sb, long offset, long size) {
        this.sb = sb;
        this.da = sb.getDeviceAccess();
        this.size = size;
        this.chars = size / 2;
        this.offset = offset;
    }

    /**
     * Gets the offset to the upcase table.
     *
     * @return the offset.
     */
    public long getOffset() {
        return offset;
    }

    /**
     * Gets the size of the upcase table.
     *
     * @return the size.
     */
    public long getSize() {
        return size;
    }

    public long checkSum() throws IOException {
        long sum = 0;

        int buffSize = 512 * 2 * 32;//32k
        // int buffSize = 512;
        ByteBuffer buff = ByteBuffer.allocate(buffSize);
        buff.order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < size; i++) {
            // sum = ((sum << 31) | (sum >> 1)) + da.getUint8(offset + i);
            int count = i / buffSize;
            if (i % buffSize == 0) {
                buff.clear();
                da.read(buff, offset + count * buffSize);
                buff.rewind();
            }

            
            int rem = i % buffSize;

            sum = ((sum << 31) | (sum >> 1)) + (buff.get(rem) & 0xff);
            sum &= 0xffffffffl;
        }

        return sum;
    }

    public char toUpperCase(char c) throws IOException {
        if (c > this.chars) {
            return c;
        } else {
            if (buffer == null) {
                cacheTable();
            }

            return getChar(c);
            // return da.getChar(offset + (c * 2));
        }
    }

    public void cacheTable() {
        if (buffer == null) {
             buffer = ByteBuffer.allocate(65536 * 2 + 512);
             buffer.order(ByteOrder.LITTLE_ENDIAN);
             try {
                da.read(buffer, offset);
                buffer.rewind();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
         
    }

    private char getChar(char c) {
        return (char)buffer.getShort(c * 2);        
    }

    public String toUpperCase(String s) throws IOException {
        final StringBuilder result = new StringBuilder(s.length());

        for (char c : s.toCharArray()) {
            result.append(toUpperCase(c));
        }

        return result.toString();
    }

    public long getCharCount() {
        return this.chars;
    }

}
