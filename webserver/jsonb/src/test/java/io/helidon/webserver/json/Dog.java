package io.helidon.webserver.json;

/**
 *
 * @author rgrecour
 */
public class Dog {

    public String name;
    public int age;
    public boolean bitable;

    @Override
    public String toString() {
        return "Dog { name = " + name 
                + ", age = " + age 
                + ", bitable = " + Boolean.toString(bitable) 
                + "}";
    }

}