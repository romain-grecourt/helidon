package io.helidon.common.io;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests {@link CompositeBuffer}.
 */
public class CompositeBufferTest {

    @Test
    public void absoluteGetByteTest() {
        CompositeBuffer buf = CompositeBuffer.create();
        buf.put("xxxfoo".getBytes());
        assertThat(buf.capacity(), is(6));
        assertThat((char) buf.get(3), is('f'));
        assertThat((char) buf.get(4), is('o'));
        assertThat((char) buf.get(5), is('o'));
        buf.put("xxx".getBytes());
        buf.put("bar".getBytes());
        assertThat(buf.capacity(), is(12));
        assertThat((char) buf.get(9), is('b'));
        assertThat((char) buf.get(10), is('a'));
        assertThat((char) buf.get(11), is('r'));
        assertThat(buf.position(), is(0));
    }

    @Test
    public void getByteTest() {
        CompositeBuffer buf = CompositeBuffer.create();
        buf.put("foo".getBytes());
        assertThat((char) buf.get(), is('f'));
        assertThat((char) buf.get(), is('o'));
        assertThat((char) buf.get(), is('o'));
        assertThat(buf.position(), is(3));
        buf.put("xxxbar".getBytes());
        assertThat(buf.capacity(), is(9));
        buf.position(6);
        assertThat((char) buf.get(), is('b'));
        assertThat((char) buf.get(), is('a'));
        assertThat((char) buf.get(), is('r'));
        assertThat(buf.position(), is(9));
    }

    @Test
    public void getBytesTest() {
        CompositeBuffer buf = CompositeBuffer.create();
        buf.put("foo".getBytes());
        assertThat(new String(buf.bytes()), is("foo"));
        assertThat(buf.position(), is(3));
        buf.put("bar".getBytes());
        assertThat(buf.capacity(), is(6));
        buf.position(0);
        assertThat(new String(buf.bytes()), is("foobar"));
        assertThat(buf.position(), is(6));
        buf.position(0);
        buf.limit(3);
        assertThat(new String(buf.bytes()), is("foo"));
        assertThat(buf.position(), is(3));
    }

    @Test
    public void positionTest() {
        CompositeBuffer buf = CompositeBuffer.create();
        buf.put("foo".getBytes());
        buf.put("bar".getBytes());
        buf.put("bob".getBytes());
        buf.put("alice".getBytes());
        assertThat(buf.capacity(), is(14));
        buf.position(6);
        assertThat(buf.position(), is(6));
        assertThat(new String(buf.bytes()), is("bobalice"));
        assertThat(buf.position(), is(14));
        buf.position(3);
        assertThat(buf.position(), is(3));
        buf.limit(6);
        assertThat(new String(buf.bytes()), is("bar"));
        assertThat(buf.position(), is(6));
        buf.limit(14);
        assertThat(new String(buf.bytes()), is("bobalice"));
        assertThat(buf.position(), is(14));
    }

    @Test
    public void deleteTest() {
        CompositeBuffer buf = CompositeBuffer.create();
        buf.put("xxxfoo".getBytes());
        assertThat(buf.capacity(), is(6));
        buf.delete(0, 3);
        assertThat(buf.capacity(), is(3));
        assertThat(new String(buf.bytes()), is("foo"));
        assertThat(buf.position(), is(3));
        buf.put("barxxx".getBytes());
        assertThat(buf.capacity(), is(9));
        buf.delete(6, 3);
        assertThat(buf.capacity(), is(6));
        assertThat(buf.position(), is(3));
        assertThat(new String(buf.bytes()), is("bar"));
        buf.delete(0, 6);
        assertThat(buf.capacity(), is(0));
        assertThat(buf.position(), is(0));
        assertThat(buf.limit(), is(0));
        buf.put("fooxxxbar".getBytes());
        assertThat(buf.capacity(), is(9));
        buf.delete(3, 3);
        assertThat(buf.position(), is(0));
        assertThat(buf.capacity(), is(6));
        assertThat(buf.limit(), is(6));
        assertThat(new String(buf.bytes()), is("foobar"));
    }

    @Test
    public void readOnlyTest() {
        CompositeBuffer buf = CompositeBuffer.create();
        buf.put("foo".getBytes());
        buf.put("bar".getBytes());
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
        CompositeBuffer buf = CompositeBuffer.create();
        buf.put("bar".getBytes());
        assertThat(buf.position(), is(0));
        assertThat(buf.capacity(), is(3));
        assertThat(buf.limit(), is(3));
        buf.put("foo".getBytes());
        assertThat(buf.position(), is(0));
        assertThat(buf.capacity(), is(6));
        assertThat(buf.limit(), is(6));
        assertThat(new String(buf.bytes()), is("foobar"));
        assertThat(buf.position(), is(6));

        buf.position(0).put("bob".getBytes());
        assertThat(buf.position(), is(3));
        assertThat(buf.capacity(), is(9));
        assertThat(buf.limit(), is(9));
        buf.position(0);
        byte[] dst = new byte[3];
        buf.get(dst);
        assertThat(new String(dst), is("bob"));
        assertThat(buf.position(), is(3));

        buf.put("alice".getBytes());
        assertThat(buf.capacity(), is(14));
        dst = new byte[5];
        buf.get(dst);
        assertThat(new String(dst), is("alice"));
        assertThat(buf.position(), is(8));

        buf.put("abz".getBytes());
        assertThat(buf.position(), is(17));
        assertThat(buf.capacity(), is(17));
        assertThat(buf.limit(), is(17));
        buf.position(14);
        assertThat(new String(buf.bytes()), is("abz"));
        assertThat(buf.position(), is(17));

        buf.position(16).put("cxy".getBytes());
        assertThat(buf.capacity(), is(20));
        buf.position(14);
        assertThat(new String(buf.bytes()), is("abcxyz"));
        assertThat(buf.position(), is(20));
    }

    @Test
    public void multipartTest() {
        CompositeBuffer buf = CompositeBuffer.create();
        buf.put(("--boundary\n"
                + "Content-Id: part1\n"
                + "\n"
                + "body 1.aaaa\n").getBytes());
        assertThat(buf.capacity(), is(42));
        byte[] dst = new byte[17];
        buf.position(11).get(dst);
        assertThat(new String(dst), is("Content-Id: part1"));
        assertThat(new String(buf.asReadOnly().position(30).limit(31).bytes()), is("b"));
        buf.delete(0, 31);
        buf.put("body 1.bbbb\n".getBytes());
        assertThat(buf.capacity(), is(23));
        assertThat(new String(buf.asReadOnly().position(0).limit(12).bytes()), is("ody 1.aaaa\nb"));

        buf.put(("body 1.cccc\n"
                + "--boundary\n"
                + "Content-Id: part2\n"
                + "\n"
                + "This is the 2nd").getBytes());
        buf.delete(0, 12);
        assertThat(buf.capacity(), is(68));
        dst = new byte[17];
        buf.position(34).get(dst);
        assertThat(new String(dst), is("Content-Id: part2"));
        assertThat(new String(buf.asReadOnly().position(0).limit(22).bytes()), is("ody 1.bbbb\nbody 1.cccc"));
        assertThat(new String(buf.asReadOnly().position(53).limit(57).bytes()), is("This"));

        buf.put((" body.\n"
                + "--boundary--").getBytes());
        buf.delete(0, 33);
        assertThat(buf.capacity(), is(54));
        assertThat(new String(buf.asReadOnly().position(24).limit(41).bytes()), is(" is the 2nd body."));
    }
}