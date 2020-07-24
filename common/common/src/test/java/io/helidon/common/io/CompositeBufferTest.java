package io.helidon.common.io;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests {@link CompositeBuffer}.
 */
public class CompositeBufferTest {

    @Test
    public void absoluteGetByteTest() {
        CompositeBuffer buf = new CompositeBuffer();
        buf.put(ByteBuffer.wrap("xxxfoo".getBytes()));
        assertThat(buf.capacity(), is(6));
        assertThat((char) buf.get(3), is('f'));
        assertThat((char) buf.get(4), is('o'));
        assertThat((char) buf.get(5), is('o'));
        buf.put(ByteBuffer.wrap("xxx".getBytes()));
        buf.put(ByteBuffer.wrap("bar".getBytes()));
        assertThat(buf.capacity(), is(12));
        assertThat((char) buf.get(9), is('b'));
        assertThat((char) buf.get(10), is('a'));
        assertThat((char) buf.get(11), is('r'));
        assertThat(buf.position(), is(0));
    }

    @Test
    public void getByteTest() {
        CompositeBuffer buf = new CompositeBuffer();
        buf.put(ByteBuffer.wrap("foo".getBytes()));
        assertThat((char) buf.get(), is('f'));
        assertThat((char) buf.get(), is('o'));
        assertThat((char) buf.get(), is('o'));
        assertThat(buf.position(), is(3));
        buf.put(ByteBuffer.wrap("xxxbar".getBytes()));
        assertThat(buf.capacity(), is(9));
        buf.position(6);
        assertThat((char) buf.get(), is('b'));
        assertThat((char) buf.get(), is('a'));
        assertThat((char) buf.get(), is('r'));
        assertThat(buf.position(), is(9));
    }

    @Test
    public void getBytesTest() {
        CompositeBuffer buf = new CompositeBuffer();
        buf.put(ByteBuffer.wrap("foo".getBytes()));
        assertThat(new String(buf.toByteArray()), is("foo"));
        assertThat(buf.position(), is(3));
        buf.put(ByteBuffer.wrap("bar".getBytes()));
        assertThat(buf.capacity(), is(6));
        buf.position(0);
        assertThat(new String(buf.toByteArray()), is("foobar"));
        assertThat(buf.position(), is(6));
        buf.position(0);
        buf.limit(3);
        assertThat(new String(buf.toByteArray()), is("foo"));
        assertThat(buf.position(), is(3));
    }

    @Test
    public void positionTest() {
        CompositeBuffer buf = new CompositeBuffer();
        buf.put(ByteBuffer.wrap("foo".getBytes()));
        buf.put(ByteBuffer.wrap("bar".getBytes()));
        buf.put(ByteBuffer.wrap("bob".getBytes()));
        buf.put(ByteBuffer.wrap("alice".getBytes()));
        assertThat(buf.capacity(), is(14));
        buf.position(6);
        assertThat(buf.position(), is(6));
        assertThat(new String(buf.toByteArray()), is("bobalice"));
        assertThat(buf.position(), is(14));
        buf.position(3);
        assertThat(buf.position(), is(3));
        buf.limit(6);
        assertThat(new String(buf.toByteArray()), is("bar"));
        assertThat(buf.position(), is(6));
        buf.limit(14);
        assertThat(new String(buf.toByteArray()), is("bobalice"));
        assertThat(buf.position(), is(14));
    }

    @Test
    public void deleteTest() {
        CompositeBuffer buf = new CompositeBuffer();
        buf.put(ByteBuffer.wrap("xxxfoo".getBytes()));
        assertThat(buf.capacity(), is(6));
        buf.delete(0, 3);
        assertThat(buf.capacity(), is(3));
        assertThat(new String(buf.toByteArray()), is("foo"));
        assertThat(buf.position(), is(3));
        buf.put(ByteBuffer.wrap("barxxx".getBytes()));
        assertThat(buf.capacity(), is(9));
        buf.delete(6, 3);
        assertThat(buf.capacity(), is(6));
        assertThat(buf.position(), is(3));
        assertThat(new String(buf.toByteArray()), is("bar"));
        buf.delete(0, 6);
        assertThat(buf.capacity(), is(0));
        assertThat(buf.position(), is(0));
        assertThat(buf.limit(), is(0));
        buf.put(ByteBuffer.wrap("fooxxxbar".getBytes()));
        assertThat(buf.capacity(), is(9));
        buf.delete(3, 3);
        assertThat(buf.position(), is(0));
        assertThat(buf.capacity(), is(6));
        assertThat(buf.limit(), is(6));
        assertThat(new String(buf.toByteArray()), is("foobar"));
    }

    @Test
    public void readOnlyTest() {
        CompositeBuffer buf = new CompositeBuffer();
        buf.put(ByteBuffer.wrap("foo".getBytes()));
        buf.put(ByteBuffer.wrap("bar".getBytes()));
        CompositeBuffer bufro = buf.asReadOnly();
        byte[] dst = new byte[3];
        buf.get(dst);
        assertThat(new String(dst), is("foo"));
        assertThat(buf.position(), is(3));
        assertThat(bufro.position(), is(0));
        dst = new byte[3];
        bufro.get(dst);
        assertThat(new String(dst), is("foo"));
        assertThat(bufro.position(), is(3));
    }

    @Test
    public void insertTest() {
        CompositeBuffer buf = new CompositeBuffer();
        buf.put(ByteBuffer.wrap("bar".getBytes()));
        assertThat(buf.position(), is(0));
        assertThat(buf.capacity(), is(3));
        assertThat(buf.limit(), is(3));
        assertThat(buf.nestedCount(), is(1));
        buf.put(ByteBuffer.wrap("foo".getBytes()));
        assertThat(buf.position(), is(0));
        assertThat(buf.capacity(), is(6));
        assertThat(buf.limit(), is(6));
        assertThat(buf.nestedCount(), is(2));
        assertThat(new String(buf.toByteArray()), is("foobar"));
        assertThat(buf.position(), is(6));

        buf.put(ByteBuffer.wrap("bob".getBytes()), 0);
        assertThat(buf.position(), is(9));
        assertThat(buf.capacity(), is(9));
        assertThat(buf.limit(), is(9));
        assertThat(buf.nestedCount(), is(3));
        buf.position(0);
        byte[] dst = new byte[3];
        buf.get(dst);
        assertThat(new String(dst), is("bob"));
        assertThat(buf.position(), is(3));

        buf.put(ByteBuffer.wrap("alice".getBytes()));
        assertThat(buf.nestedCount(), is(4));
        assertThat(buf.capacity(), is(14));
        dst = new byte[5];
        buf.get(dst);
        assertThat(new String(dst), is("alice"));
        assertThat(buf.position(), is(8));

        buf.put(ByteBuffer.wrap("abz".getBytes()), 14);
        assertThat(buf.position(), is(8));
        assertThat(buf.capacity(), is(17));
        assertThat(buf.limit(), is(17));
        assertThat(buf.nestedCount(), is(5));
        buf.position(14);
        assertThat(new String(buf.toByteArray()), is("abz"));
        assertThat(buf.position(), is(17));

        buf.put(ByteBuffer.wrap("cxy".getBytes()), 16);
        assertThat(buf.nestedCount(), is(7));
        assertThat(buf.capacity(), is(20));
        buf.position(14);
        assertThat(new String(buf.toByteArray()), is("abcxyz"));
        assertThat(buf.position(), is(20));
    }

    @Test
    public void multipartTest() {
        CompositeBuffer buf = new CompositeBuffer();

        buf.put(ByteBuffer.wrap(("--boundary\n"
                + "Content-Id: part1\n"
                + "\n"
                + "body 1.aaaa\n").getBytes()));
        assertThat(buf.capacity(), is(42));
        byte[] dst = new byte[17];
        buf.position(11).get(dst);
        assertThat(new String(dst), is("Content-Id: part1"));
        assertThat(new String(buf.asReadOnly().position(30).limit(31).toByteArray()), is("b"));
        buf.delete(0, 31);
        buf.put(ByteBuffer.wrap("body 1.bbbb\n".getBytes()));
        assertThat(buf.capacity(), is(23));
        assertThat(new String(buf.asReadOnly().position(0).limit(12).toByteArray()), is("ody 1.aaaa\nb"));

        buf.put(ByteBuffer.wrap(("body 1.cccc\n"
                + "--boundary\n"
                + "Content-Id: part2\n"
                + "\n"
                + "This is the 2nd").getBytes()));
        buf.delete(0, 12);
        assertThat(buf.capacity(), is(68));
        assertThat(buf.nestedCount(), is(2));
        dst = new byte[17];
        buf.position(34).get(dst);
        assertThat(new String(dst), is("Content-Id: part2"));
        assertThat(new String(buf.asReadOnly().position(0).limit(22).toByteArray()), is("ody 1.bbbb\nbody 1.cccc"));
        assertThat(new String(buf.asReadOnly().position(53).limit(57).toByteArray()), is("This"));

        buf.put(ByteBuffer.wrap((" body.\n"
                + "--boundary--").getBytes()));
        buf.delete(0, 33);
        assertThat(buf.capacity(), is(54));
        assertThat(buf.nestedCount(), is(2));
        assertThat(new String(buf.asReadOnly().position(24).limit(41).toByteArray()), is(" is the 2nd body."));
    }
}