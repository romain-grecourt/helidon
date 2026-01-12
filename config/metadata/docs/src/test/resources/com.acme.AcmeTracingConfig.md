
# com.acme.AcmeTracingConfig

## Description

ACME Tracing configuration

## Usages

<ul class="usages">
    <li><a href="com.acme.AcmeServerConfig.md#features"><code>server.features</code></a></li>
</ul>

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
        <tr id="host">
            <td><code>host</code></td>
            <td><code>String</code></td>
            <td><code>VALUE</code></td>
            <td>Tracing backend host</td>
            <td>
                <ul>
                    <li>required</li>
                </ul>
            </td>
            <td>-</td>
        </tr>
        <tr id="port">
            <td><code>port</code></td>
            <td><code>Integer</code></td>
            <td><code>VALUE</code></td>
            <td>Tracing backend port</td>
            <td>
                <ul>
                    <li>optional</li>
                </ul>
            </td>
            <td><code>16686</code></td>
        </tr>
        <tr id="tags">
            <td><code>tags</code></td>
            <td><code>String</code></td>
            <td><code>LIST</code></td>
            <td>System tags</td>
            <td>
                <ul>
                    <li>required</li>
                </ul>
            </td>
            <td>-</td>
        </tr>
    </tbody>
</table>

---

See the [manifest](manifest.md) for all available types.
