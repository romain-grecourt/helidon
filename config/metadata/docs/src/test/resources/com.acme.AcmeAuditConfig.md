
# com.acme.AcmeAuditConfig

## Description

ACME Audit configuration

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
        <tr id="paths">
            <td><code>paths</code></td>
            <td><code>String</code></td>
            <td><code>MAP</code></td>
            <td>Audited paths</td>
            <td>
                <ul>
                    <li>required</li>
                </ul>
            </td>
            <td>-</td>
        </tr>
        <tr id="strict">
            <td><code>strict</code></td>
            <td><code>Boolean</code></td>
            <td><code>VALUE</code></td>
            <td>Fail on error</td>
            <td>
                <ul>
                    <li>required</li>
                </ul>
            </td>
            <td>-</td>
        </tr>
    </tbody>
</table>
