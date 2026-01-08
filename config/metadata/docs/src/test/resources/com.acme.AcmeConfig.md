
# com.acme.AcmeConfig

## Description

ACME configuration.

## Usages

This config type is standalone and configured under the root key `acme`.

## Options

<table>
    <thead>
        <tr>
            <th>Key</th>
            <th>Type</th>
            <th>Description</th>
            <th>Flags</th>
            <th>Default Value</th>
            <th colspan="2">Allowed Values</th>
        </tr>
    </thead>
    <tbody>
        <tr id="mode">
            <td rowspan="3"><code>mode</code></td>
            <td rowspan="3"><code>String</code></td>
            <td rowspan="3">Mode</td>
            <td rowspan="3">
                <ul>
                    <li>optional</li>
                </ul>
            </td>
            <td rowspan="3"><code>MODE2</code></td>
            <td><code>MODE1</code></td>
            <td>mode 1</td>
        </tr>
        <tr>
            <td><code>MODE2</code></td>
            <td>mode 2</td>
        </tr>
        <tr>
            <td><code>MODE3</code></td>
            <td>mode 3</td>
        </tr>
        <tr id="option1">
            <td rowspan="1"><code>option1</code></td>
            <td rowspan="1"><a href="com.acme.AcmeOptionConfig.md"><code>AcmeOptionConfig</code></a></td>
            <td rowspan="1">Option 1</td>
            <td rowspan="1">
                <ul>
                    <li>required</li>
                    <li>deprecated</li>
                </ul>
            </td>
            <td rowspan="1">-</td>
            <td colspan="2">-</td>
        </tr>
        <tr id="option2">
            <td rowspan="1"><code>option2</code></td>
            <td rowspan="1"><a href="com.acme.AcmeOptionConfig.md"><code>AcmeOptionConfig</code></a></td>
            <td rowspan="1">Option 2</td>
            <td rowspan="1">
                <ul>
                    <li>required</li>
                    <li>experimental</li>
                </ul>
            </td>
            <td rowspan="1">-</td>
            <td colspan="2">-</td>
        </tr>
    </tbody>
</table>
