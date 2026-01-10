
# com.acme.AcmeServerConfig

## Description

ACME Server configuration.

## Usages

This type is standalone and configured under the root key `server`.

## Options

<table>
    <thead>
        <tr>
            <th>Key</th>
            <th>Type</th>
            <th>Kind</th>
            <th>Description</th>
            <th>Flags</th>
            <th>Default Value</th>
        </tr>
    </thead>
    <tbody>
        <tr id="features">
            <td><code>features</code></td>
            <td><a href="com.acme.AcmeFeature.md"><code>AcmeFeature</code></a></td>
            <td><code>LIST</code></td>
            <td>Dynamic features</td>
            <td>
                <ul>
                    <li>required</li>
                    <li>provider</li>
                </ul>
            </td>
            <td>-</td>
        </tr>
        <tr id="host">
            <td><code>host</code></td>
            <td><code>String</code></td>
            <td><code>VALUE</code></td>
            <td>Listen address</td>
            <td>
                <ul>
                    <li>optional</li>
                </ul>
            </td>
            <td><code>localhost</code></td>
        </tr>
        <tr id="port">
            <td><code>port</code></td>
            <td><code>Integer</code></td>
            <td><code>VALUE</code></td>
            <td>Listen port</td>
            <td>
                <ul>
                    <li>optional</li>
                </ul>
            </td>
            <td><code>8080</code></td>
        </tr>
        <tr id="sockets">
            <td><code>sockets</code></td>
            <td><a href="com.acme.AcmeListenerConfig.md"><code>AcmeListenerConfig</code></a></td>
            <td><code>MAP</code></td>
            <td>Sockets</td>
            <td>
                <ul>
                    <li>required</li>
                </ul>
            </td>
            <td>-</td>
        </tr>
    </tbody>
</table>
