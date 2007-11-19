<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:template match="/dbitracker">
  <html>
  <head>
    <meta http-equiv="refresh" content="30;?page={key}"/>
    <link rel="stylesheet" type="text/css" href="/style.css"/>
    <title>JSnap Web Console</title>
  </head>
  <body>
    <p>Reported on: <i><xsl:value-of select="now"/></i></p>
    <xsl:choose>
    <xsl:when test="count(instance)=0">
    <p>There are not any pooled connections.</p>
    </xsl:when>
    <xsl:otherwise>
    <table cellspacing="0" cellpadding="4">
    <tr>
	<td class="info"><b>#</b></td>
    <td class="info"><b>Database</b></td>
    <td class="info"><b>Connected Until</b></td>
    </tr>
    <xsl:variable name="databases" select="//database[not(.=following::database)]"/>
    <xsl:for-each select="$databases">
      <xsl:sort select="."/>
      <xsl:variable name="current" select="."/>
      <!--tr><td colspan="3"><xsl:value-of select="$current"/></td></tr-->
      <xsl:variable name="oddeven" select="position() mod 2"/>
      <xsl:variable name="bgcolor">
        <xsl:choose>
          <xsl:when test="$oddeven=0">darkgray</xsl:when>
          <xsl:otherwise>lightgray</xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:for-each select="//instance[database=$current]">
        <tr>
        <td class="{$bgcolor}"><xsl:number value="position()" format="1"/></td>
        <td class="{$bgcolor}"><xsl:value-of select="database"/></td>
        <td class="{$bgcolor}"><xsl:value-of select="until"/></td>
        </tr>
      </xsl:for-each>
    </xsl:for-each>
    </table>
    </xsl:otherwise>
    </xsl:choose>
  </body>
  </html>
  </xsl:template>
</xsl:stylesheet>
