
# com.acme.AcmeLoggerConfig

## Description

ACME Logger configuration

## Usages

<ul class="usages">
    <li><a href="com.acme.AcmeLoggingConfig.md#loggers"><code>server.features.logging.loggers</code></a></li>
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
            <th>Default Value</th><th colspan="2">Allowed Values</th>
        </tr>
    </thead>
    <tbody>
        <tr id="level">
            <td rowspan="4"><code>level</code></td>
            <td rowspan="4"><code>String</code></td>
            <td rowspan="4"><code>VALUE</code></td>
            <td rowspan="4">Logging level</td>
            <td rowspan="4">
                <ul>
                    <li>optional</li>
                </ul>
            </td>
            <td rowspan="4"><code>INFO</code></td>
            <td><code>ERROR</code></td>
            <td>Error level</td>
        </tr>
        <tr>
            <td><code>WARNING</code></td>
            <td>Warning level</td>
        </tr>
        <tr>
            <td><code>INFO</code></td>
            <td>Info level</td>
        </tr>
        <tr>
            <td><code>DEBUG</code></td>
            <td>Debug level</td>
        </tr>
        <tr id="logger">
            <td><code>logger</code></td>
            <td><code>String</code></td>
            <td><code>VALUE</code></td>
            <td>Logger name</td>
            <td>
                <ul>
                    <li>required</li>
                </ul>
            </td>
            <td>-</td>
            <td colspan="2">-</td>
        </tr>
    </tbody>
</table>
