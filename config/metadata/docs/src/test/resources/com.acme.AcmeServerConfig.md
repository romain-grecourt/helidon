
# com.acme.AcmeServerConfig

## Description

ACME Server configuration.

## Usages

<ul class="usages">
    <li><a href="README.md#server"><code>server</code></a></li>
</ul>

## Options

<style>
    code {
        white-space: nowrap !important;
    }
</style>

<table>
    <thead>
        <tr>
            <th>Key</th>
            <th>Kind</th>
            <th>Type</th><th>Default Value</th><th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr id="features">
            <td><a href="com.acme.AcmeFeature.md"><code>features</code></a></td>
            <td><code>LIST</code></td>
            <td><code>c.a.AcmeFeature</code></td>
            <td></td>
            <td>Dynamic features</td>
        </tr>
        <tr id="host">
            <td><code>host</code></td>
            <td><code>VALUE</code></td>
            <td><code>String</code></td>
            <td><code>localhost</code></td>
            <td>Listen address</td>
        </tr>
        <tr id="port">
            <td><code>port</code></td>
            <td><code>VALUE</code></td>
            <td><code>Integer</code></td>
            <td><code>8080</code></td>
            <td>Listen port</td>
        </tr>
        <tr id="sockets">
            <td><a href="com.acme.AcmeListenerConfig.md"><code>sockets</code></a></td>
            <td><code>MAP</code></td>
            <td><code>c.a.AcmeListenerConfig</code></td>
            <td></td>
            <td>Sockets</td>
        </tr>
    </tbody>
</table>

---

See the [manifest](manifest.md) for all available types.
