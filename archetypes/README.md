# Archetype Engine v2

This document describes the design of Helidon archetype engine V2.

## Introduction

This new version of the archetype engine will provide the low-level archetype support needed for the project
 [starter](https://github.com/batsatt/helidon-build-tools/blob/starter/starter-proposal/starter-proposal.md).

V1 had a concept of input flow that models user inputs.

V2 expands that concept into an advanced graph of inputs, logically grouped as steps. The inputs graph is mirrored by a
 graph of choices that reflects the current answers to the inputs. Files and templates can be scoped at any level of
 the inputs graph, and can also use conditional expressions that query the choices graph.

V2 enables the modeling of fined grained "features" by creating logical groups of user inputs and templates that can be
 re-used in throughout the inputs graph.

V1 has a concept of a catalog that aggregates multiple standalone archetypes, instead V2 uses a mono archetype
 that encapsulates all possible choices.

The mono archetype is a single project, which provides significant benefits:
- easier to maintain, since all files are co-located
- easier to understand
- easier for sharing across the inputs graph

## Descriptor

V2 will also use an XML descriptor, it may look similar to the V1 descriptors however it is completely different and
 incompatible. The top-level element is changed to reflect that.

Since the concept of V2 are more advanced, the descriptor is more complex and requires more understand from the
 archetype maintainers. To further allow for logical grouping of features, descriptors can be broken up and "included".

An XML schema will be provided for IDE documentation and auto-completion. Some parts of the descriptors are designed
 specifically so that the schema can indicate what elements can be used.

See below a skeleton of the new XML descriptor:

```xml
<archetype-flow xmlns="https://helidon.io/archetype/2.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="https://helidon.io/archetype/2.0 https://helidon.io/xsd/archetype-flow-2.0.xsd">

    <choice flow="">
        <option value="" />
    </choice>
    <invoke-flow src="" />
    <include src="" />
    <flow-step label="" if="">
        <flow-input />
        <output />
    </flow-step>
    <flow-input name="" type="" label="" prompt="">
        <option value="" label="">
            <invoke-flow src="" />
            <include src="" />
            <flow-step />
            <flow-input />
            <output />
        </option>
        <invoke-flow src="" />
        <include src="" />
        <output />
    </flow-input>
    <output>
        <transformation id="">
            <replace regex="" replacement=""/>
        </transformation>
        <templates transformations="">
            <directory></directory>
            <includes>
                <include></include>
            </includes>
        </templates>
        <templates transformations="">
            <directory></directory>
            <includes>
                <include></include>
            </includes>
        </templates>
        <files transformations="">
            <directory></directory>
            <excludes>
                <exclude></exclude>
            </excludes>
        </files>
        <model>
            <item id="" order="" if=""></item>
            <list id="" order="" if="">
                <item id="" order="" if="">
                    <map id="" order="" if="">
                        <entry key=""></entry>
                    </map>
                </item>
            </list>
            <map id="" order="" if="">
                <entry key=""></entry>
            </map>
        </model>
    </output>
</archetype-flow>
```
## Decoupling descriptors

Two directives elements are provided to decouple descriptors:
 - `<invoke-flow src="path-to-xml" />`
 - `<include src="path-to-xml" />`
 
The value of the path attribute is always relative to the current descriptor.

`<invoke-flow>` invokes the flow declared in a separate descriptor within the current flow step/input.
 It effectively changes the current working directory to the directory that contains the invoked xml file.
All references are resolved relative to their own directory.

`<include>` allows to re-use an XML file in a different working directory.
It includes all non flow-* elements in the current context.

Both directives can be used inside the following elements
 - `<archetype-flow>`
 - `<flow-step>`
 - `<flow-input>`
 - `<option>`

## Flow Input

The inputs graph is a DAG (direct acyclic graph) formed by way of nesting `<flow-input>` elements. `<flow-input>`
 requires a `name` attribute that must be unique among the children of the parent `<flow-input>` element.

An input can be of different types:
- option: opt-in by default, but can be declared as opt-out
- select: single optional choice by default, but can be required and or multiple
- text: text value

Example of a selection:
```
<flow-input name="flavor"
            type="select"
            label="Select a flavor">

    <option value="se" label="Helidon SE" />
    <option value="mp" label="Helidon MP" />
</flow-input>
```

Example of an option:
```
<flow-input name="kubernetes"
            type="option"
            label="Kubernetes Support"
            question-label="Do you want support for Kubernetes"/>
```

Example of a text value:
```
<flow-input name="name"
            type="text"
            label="Project name"
            placeholder="myproject"/>
```

## Flow Step

A flow step represents a UX pane or windows that contains certain set of inputs.
Steps are required by default and can be made optional using the optional attribute (`optional="true`).

```
<flow-step label="Application Type">
    <!-- ... -->
</flow-step>
```

### Optional steps

Optional steps must have inputs with defaults. An optional step with non default inputs is invalid and should be
 validated as an error.

Customization of features can be modeled using an optional step declared as the last child of the step to be
 customized. The order is the declaration order, the enclosing steps must declare the optional step used for
 customization carefully.

E.g.
- `se/se.xml` invokes `se/hello-world/hello-world-se.xml` AND THEN invokes `common/customize-project.xml`
- `se/hello-world/hello-world-se.xml` invokes `se/hello-world/customize-hello-world.xml` as the very last step.

The declaration order for the two customization steps is first hello-world and then project.

When resolving flow nodes, the next steps are always computed. If next steps are all optional, it must possible to
 skip them and generate the project.

An optional step requires no action other than continuing to the next step (unless this is the very last step).

### Continue action

If there is any "next" step, a `CONTINUE` action is provided to navigate to the next step.
If the step is non optional and has non defaulted inputs, the `CONTINUE` action is disabled (greyed out), and enabled
 when the inputs have been filled.
When the current step is optional, the `CONTINUE` action is available and can be used to skip.

## Choices graph

The choices graph represents the current choices made by the user. It is maintained and evolves as the user makes
 choices.

E.g. Parts of the tree may be removed if a user changes a previous choice.

```
|- flavor=se
    |- base=bare
        |- media-support/provider
             |- jackson
        |- security
            |- authentication/provider
                |- basic-auth
```

## Choices path

A path can be used to point at a node in the choices graph.

E.g.
- `flavor/base`
- `media-support/provider`
- `security/authentication/provider`

If relative, the path applies to the current context. An absolute path starts with a `/`.

Invalid paths should be validated at build time and reported as errors.

## Choice

Choices can be used to pre-fill the choices graph before flow resolution. When resolving flow, entries that exist in
 the choices graph will be skipped, those are immutable and hidden.

Choices can also be passed to the archetype engine "manually", e.g. batch cli, URI query param.

The `flow` attribute is used to specify the path in the choices graph to be filled.

The example choices below are effectively "presets".

```
<choice flow="media-support/json/provider"> <!-- example of a single value choice -->
    <option value="jackson" />
</choice>
<choice flow="security/authentication/provider"> <!-- example of a multi-value choice (i.e a select with multiple values) -->
    <option value="basic-auth" />
    <option value="digest-auth" />
</choice>
<choice flow="health"> <!-- example of opting out of an option that defaults to true -->
    <option value="false" />
</choice>
<choice flow="project-name">my-super-project</choice> <!-- example of a text choice -->
```

## Choices expressions

Choices expression are boolean expressions that can be used to query the choices graph.

Operands can use `${}` to specify a path in the choices graph.
E.g. 
- `${media-support/json/provider}`
- `${security/authentication/provider}`
- `${security}`

At build time, the expressions are parsed and validated:
- paths must be valid in the current context
- operators must be valid (E.g. `== true` is invalid be used on a text option or select option)

An expression with a single operand will test if an input is set in the choices graph, regardless of the type of input:
- `${security}`

The operator `==` can be used to test equality:
- `${security} == true`
- `${media-support/json/provider} == jackson`

The operator `!=` can be used to test equality:
- `${media-support/json/provider} != jackson`

The operators `&&` and `||` can be used for logical AND / OR:
- `${security} && ${media-support}`
- `${security} || ${media-support}`

The operator `contains` can be used to test if a multiple select contains a value:
- `${security/authentication/provider} contains basic-auth`

The operator `!` to negate sub expressions, parenthesis can be used to group expressions:
- `!(${security} && ${media-support}) || ${health}`

Choices expressions are in the following elements:
- any child of `<model>`
- `<flow-step if="expr">`

## Choices intersection

The intersections different parts of the choices graph can be done by using choices expressions on a step.

```
<flow-step label="Jersey Security" if="${security}">
    <flow-input name="username"
                type="text"
                label="Username"
                prompt="What username do you want to use"
                placeholder="myuser">
    </flow-input>
</flow-step>
```

### Choices URI mapping

The example below maps the choices shown above into query parameters.

```
?media-support|json|provider=jackson&security|authentication|provider=basic-auth,digest-auth&health=false&project-name=my-super-project
```

This will be used to provide standalone links to generate projects.

## Output

TODO `<output>`

## Output model

TODO

-----------------

## UI wizard mock-up

```
(1) Application Type
 |
 | Select a type of application:
 |  ( ) Bare
 |  ( ) Hello World
 |  ( ) Database
 |
 | [CONTINUE]
-----------------------------------------
(2) Kubernetes
 |
 | [ ] Kubernetes support
 |   |- [ x ] add a service
 |   |- [ x ] add an Istio sidecar
 |
 | [CONTINUE]
-----------------------------------------
(3) Media Support
 |
 | [ x ] Media type support
 |   |- [ x ] JSON
 |        |- [ x ] Jackson
 |        |- [ x ] JSON-B
 |        |- [ x ] JSON-P
 |   |- [ x ] XML
 |        |- [ x ] JAX-B
 |        |- [ x ] JAX-P
 |
 | [CONTINUE]
-----------------------------------------
(4) Customize Project
 |
 |   Project name [ my-project ]
 |   Project groupId [ com.example ]
 |   Project artifactId [ my-project ]
 |   Project version [ 1.0-SNAPSHOT ]
 |   Java package [ com.example.myproject ]
 |
-----------------------------------------
[TRY IT!]
```

## Build time processing

## Archive

TODO:
- packaging
- future optimization (store serialized objects instead of XML)

## Maven compatibility

TODO:
- Generate artifacts with generated poms.
- Add dependencies
- Maybe create a separate maven-compat artifact