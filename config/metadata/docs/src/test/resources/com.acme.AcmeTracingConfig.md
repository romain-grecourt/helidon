
# com.acme.AcmeTracingConfig

## Description

ACME Tracing configuration

## Usages

<ul class="usages">
    <li><a href="com.acme.AcmeFeature.md#tracing"><code>server.features.tracing</code></a></li>
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
        <tr id="host">
            <td><code>host</code></td>
            <td><code>VALUE</code></td>
            <td><code>String</code></td><td></td><td>Tracing backend host</td>
        </tr>
        <tr id="port">
            <td><code>port</code></td>
            <td><code>VALUE</code></td>
            <td><code>Integer</code></td><td><code>16686</code></td><td>Tracing backend port</td>
        </tr>
        <tr id="tags">
            <td><code>tags</code></td>
            <td><code>LIST</code></td>
            <td><code>String</code></td><td></td><td>System tags</td>
        </tr>
    </tbody>
</table>

---

See the [manifest](manifest.md) for all available types.
