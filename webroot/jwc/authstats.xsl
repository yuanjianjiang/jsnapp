<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:template match="/auth">
  <html>
  <head>
    <link rel="stylesheet" type="text/css" href="/style.css"/>
    <title>JSnap Web Console</title>
    <script type="text/javascript" src="/jwc/commons.js"/>
    <script type="text/javascript">
    &lt;!--
    function countVisible() {
      count = 0;
      if (document.query.filter1state.value == 'on')
        count = count + 1;
      if (document.query.filter2state.value == 'on')
        count = count + 1;
      if (document.query.filter3state.value == 'on')
        count = count + 1;
      if (document.query.filter4state.value == 'on')
        count = count + 1;
      if (document.query.filter5state.value == 'on')
        count = count + 1;
      if (document.query.filter6state.value == 'on')
        count = count + 1;
      return count;
    }

    function removeFilter(filter) {
      if (filter == 'filter1')
        document.query.filter1state.value = 'off';
      else if (filter == 'filter2')
        document.query.filter2state.value = 'off';
      else if (filter == 'filter3')
        document.query.filter3state.value = 'off';
      else if (filter == 'filter4')
        document.query.filter4state.value = 'off';
      else if (filter == 'filter5')
        document.query.filter5state.value = 'off';
      else if (filter == 'filter6')
        document.query.filter6state.value = 'off';
      else
        return;
      invisible(filter);
      count = countVisible();
      if (count == 5)
        visible('filteradd');
    }

    function addFilter() {
      count = countVisible();
      if (count == 5)
        invisible('filteradd');
      else if (count == 6)
        return;
      if (document.query.filter1state.value == 'off') {
        document.query.filter1state.value = 'on';
        visible('filter1');
      } else if (document.query.filter2state.value == 'off') {
        document.query.filter2state.value = 'on';
        visible('filter2');
      } else if (document.query.filter3state.value == 'off') {
        document.query.filter3state.value = 'on';
        visible('filter3');
      } else if (document.query.filter4state.value == 'off') {
        document.query.filter4state.value = 'on';
        visible('filter4');
      } else if (document.query.filter5state.value == 'off') {
        document.query.filter5state.value = 'on';
        visible('filter5');
      } else if (document.query.filter6state.value == 'off') {
        document.query.filter6state.value = 'on';
        visible('filter6');
      }
    }

    function init() {
      if (document.query.filter1state.value == 'off')
        invisible('filter1');
      if (document.query.filter2state.value == 'off')
        invisible('filter2');
      if (document.query.filter3state.value == 'off')
        invisible('filter3');
      if (document.query.filter4state.value == 'off')
        invisible('filter4');
      if (document.query.filter5state.value == 'off')
        invisible('filter5');
      if (document.query.filter6state.value == 'off')
        invisible('filter6');
      count = countVisible();
      if (count == 6)
        invisible('filteradd');
    }
    // --&gt;
    </script>
  </head>
  <body onload="init();">
    <xsl:call-template name="query"/>
  	<table width="100%" cellspacing="0" cellpadding="4">
    <xsl:choose>
      <xsl:when test="error">
      <font color="#ff0000">
      <tr>
        <td class="error">
          Make sure that the applied filters contain proper values.<br/>
          Timestamp values are expected in "dd/mm/yyyy hh:mm:ss" format.
        </td>
      </tr>
      </font>
      </xsl:when>
      <xsl:when test="total &gt; 0">
        <xsl:choose>
          <xsl:when test="total &gt; 1">
            <tr><td>Matched a total of <xsl:number value="total" grouping-size="3" grouping-separator=","/> attempts.</td></tr>
          </xsl:when>
          <xsl:otherwise>
            <tr><td>Matched a single attempt.</td></tr>
          </xsl:otherwise>
        </xsl:choose>
        <xsl:if test="count(group) &gt; 0">
          <xsl:call-template name="groupby"/>
        </xsl:if>
        <xsl:if test="pages &gt; 1">
          <xsl:call-template name="pages"/>
        </xsl:if>
      </xsl:when>
      <xsl:otherwise>
        <tr><td>No records matched the search criteria.</td></tr>
      </xsl:otherwise>
    </xsl:choose>
    </table>
  </body>
  </html>
  </xsl:template>

  <xsl:template name="query">
    <form name="query" method="post" action="main.do">
      <table width="100%" cellspacing="0" cellpadding="4" bgcolor="#eeeeee">
      <input type="hidden" name="refresh" value="true"/>
      <input type="hidden" name="page" value="{key}"/>
      <tr>
      <td>
      <table>
        <tr>
          <td><b>Records Per Page:</b></td>
          <td colspan="2">
          <select name="perpage">
            <xsl:call-template name="option">
              <xsl:with-param name="tested"><xsl:value-of select="perpage"/></xsl:with-param>
              <xsl:with-param name="key">10</xsl:with-param>
              <xsl:with-param name="text">10</xsl:with-param>
            </xsl:call-template>
            <xsl:call-template name="option">
              <xsl:with-param name="tested"><xsl:value-of select="perpage"/></xsl:with-param>
              <xsl:with-param name="key">20</xsl:with-param>
              <xsl:with-param name="text">20</xsl:with-param>
            </xsl:call-template>
            <xsl:call-template name="option">
              <xsl:with-param name="tested"><xsl:value-of select="perpage"/></xsl:with-param>
              <xsl:with-param name="key">50</xsl:with-param>
              <xsl:with-param name="text">50</xsl:with-param>
            </xsl:call-template>
          </select>
          </td>
        </tr>
        <tr>
          <td><b>Group By:</b></td>
          <td colspan="2">
            <select name="groupby">
              <xsl:call-template name="option">
                <xsl:with-param name="tested"><xsl:value-of select="groupby"/></xsl:with-param>
                <xsl:with-param name="key">username</xsl:with-param>
                <xsl:with-param name="text">Username</xsl:with-param>
              </xsl:call-template>
              <xsl:if test="key = 'stats-auth'">
                <xsl:call-template name="option">
                  <xsl:with-param name="tested"><xsl:value-of select="groupby"/></xsl:with-param>
                  <xsl:with-param name="key">dbname</xsl:with-param>
                  <xsl:with-param name="text">Database</xsl:with-param>
                </xsl:call-template>
              </xsl:if>
              <xsl:call-template name="option">
                <xsl:with-param name="tested"><xsl:value-of select="groupby"/></xsl:with-param>
                <xsl:with-param name="key">ipaddress</xsl:with-param>
                <xsl:with-param name="text">IP Address</xsl:with-param>
              </xsl:call-template>
              <xsl:call-template name="option">
                <xsl:with-param name="tested"><xsl:value-of select="groupby"/></xsl:with-param>
                <xsl:with-param name="key">result</xsl:with-param>
                <xsl:with-param name="text">Result</xsl:with-param>
              </xsl:call-template>
              <xsl:call-template name="option">
                <xsl:with-param name="tested"><xsl:value-of select="groupby"/></xsl:with-param>
                <xsl:with-param name="key">minute</xsl:with-param>
                <xsl:with-param name="text">Login Time (Minute)</xsl:with-param>
              </xsl:call-template>
              <xsl:call-template name="option">
                <xsl:with-param name="tested"><xsl:value-of select="groupby"/></xsl:with-param>
                <xsl:with-param name="key">hour</xsl:with-param>
                <xsl:with-param name="text">Login Time (Hour)</xsl:with-param>
              </xsl:call-template>
              <xsl:call-template name="option">
                <xsl:with-param name="tested"><xsl:value-of select="groupby"/></xsl:with-param>
                <xsl:with-param name="key">day</xsl:with-param>
                <xsl:with-param name="text">Login Time (Day)</xsl:with-param>
              </xsl:call-template>
              <xsl:call-template name="option">
                <xsl:with-param name="tested"><xsl:value-of select="groupby"/></xsl:with-param>
                <xsl:with-param name="key">month</xsl:with-param>
                <xsl:with-param name="text">Login Time (Month)</xsl:with-param>
              </xsl:call-template>
              <xsl:call-template name="option">
                <xsl:with-param name="tested"><xsl:value-of select="groupby"/></xsl:with-param>
                <xsl:with-param name="key">year</xsl:with-param>
                <xsl:with-param name="text">Login Time (Year)</xsl:with-param>
              </xsl:call-template>
            </select>
          </td>
        </tr>
        <tr>
          <td><b>Result:</b></td>
          <xsl:choose>
            <xsl:when test="successful">
                <td><input class="checkbox" type="checkbox" name="successful" checked="true"/><label>Successful</label></td>
            </xsl:when>
            <xsl:otherwise>
              <td><input class="checkbox" type="checkbox" name="successful"/><label>Successful</label></td>
            </xsl:otherwise>
          </xsl:choose>
          <xsl:choose>
            <xsl:when test="failed">
              <td><input class="checkbox" type="checkbox" name="failed" checked="true"/><label>Failed</label></td>
            </xsl:when>
            <xsl:otherwise>
              <td><input class="checkbox" type="checkbox" name="failed"/><label>Failed</label></td>
            </xsl:otherwise>
          </xsl:choose>
        </tr>
      </table>
      <xsl:for-each select="filter">
        <xsl:call-template name="filter">
          <xsl:with-param name="id"><xsl:number count="filter"/></xsl:with-param>
          <xsl:with-param name="field"><xsl:value-of select="field"/></xsl:with-param>
          <xsl:with-param name="operator"><xsl:value-of select="operator"/></xsl:with-param>
          <xsl:with-param name="value"><xsl:value-of select="value"/></xsl:with-param>
        </xsl:call-template>
      </xsl:for-each>
      <table>
        <tr><td><a id="filteradd" href="#" onclick="addFilter();return false;"><small>Add new filter</small></a></td></tr>
        <tr><td><input class="button" type="submit" value="Execute"/></td></tr>
      </table>
      </td>
      </tr>
      <tr><td align="right" bgcolor="#ffffff">Reported on: <i><xsl:value-of select="now"/></i></td></tr>
      </table>
    </form>
  </xsl:template>

  <xsl:template name="groupby">
    <tr><td>
      <table cellspacing="0" cellpadding="4">
        <xsl:variable name="imgwidth" select="300"/>
        <xsl:variable name="total" select="total"/>
        <xsl:variable name="times" select="count(group/lastsucceeded) + count(group/lastfailed)"/>
        <tr>
          <td>
            <b>
            <xsl:call-template name="keytotext">
              <xsl:with-param name="key"><xsl:value-of select="groupby"/></xsl:with-param>
            </xsl:call-template>
            </b>
          </td>
          <xsl:if test="$times &gt; 0">
            <td><b>Last Succeeded</b></td>
            <td><b>Last Failed</b></td>
          </xsl:if>
          <td align="right"><b>Attempts</b></td>
          <td align="right"><xsl:if test="$total &gt; 0"><b>Percentage</b></xsl:if></td>
          <td></td>
        </tr>
        <xsl:for-each select="group">
        <xsl:sort select="counter" data-type="number" order="descending"/>
        <xsl:sort select="title"/>
        <xsl:variable name="percentage" select="counter div $total * 100"/>
        <xsl:variable name="percentageInt" select="floor(counter div $total * 100)"/>
        <tr>
          <td><xsl:value-of select="title"/></td>
          <xsl:if test="$times &gt; 0">
            <td><xsl:value-of select="lastsucceeded"/></td>
            <td><xsl:value-of select="lastfailed"/></td>
          </xsl:if>
          <td align="right"><xsl:value-of select="counter"/></td>
          <td align="right"><xsl:value-of select="format-number($percentage, '0.00')"/>%</td>
          <td><xsl:if test="$percentageInt&gt;0"><img src="/percentage?p={$percentageInt}&amp;w={$imgwidth}" alt="{$percentage}%.png"/></xsl:if></td>
        </tr>
        </xsl:for-each>
      </table>
    </td></tr>
  </xsl:template>

  <xsl:template name="filter">
    <xsl:param name="id"/>
    <xsl:param name="field"/>
    <xsl:param name="operator"/>
    <xsl:param name="value"/>
    <xsl:choose>
      <xsl:when test="$field != ''">
        <input type="hidden" name="filter{$id}state" value="on"/>
      </xsl:when>
      <xsl:otherwise>
        <input type="hidden" name="filter{$id}state" value="off"/>
      </xsl:otherwise>
    </xsl:choose>
    <table id="filter{$id}">
      <tr>
        <td><a href="#" onclick="removeFilter('filter{$id}');return false;"><small>Remove</small></a></td>
        <td><b>Filter:</b></td>
        <td>
          <select name="field{$id}">
            <xsl:call-template name="option">
              <xsl:with-param name="tested"><xsl:value-of select="$field"/></xsl:with-param>
              <xsl:with-param name="key">none</xsl:with-param>
              <xsl:with-param name="text">(field)</xsl:with-param>
            </xsl:call-template>
            <xsl:call-template name="option">
              <xsl:with-param name="tested"><xsl:value-of select="$field"/></xsl:with-param>
              <xsl:with-param name="key">username</xsl:with-param>
              <xsl:with-param name="text">Username</xsl:with-param>
            </xsl:call-template>
            <xsl:if test="../key = 'stats-auth'">
              <xsl:call-template name="option">
                <xsl:with-param name="tested"><xsl:value-of select="$field"/></xsl:with-param>
                <xsl:with-param name="key">dbname</xsl:with-param>
                <xsl:with-param name="text">Database</xsl:with-param>
              </xsl:call-template>
            </xsl:if>
            <xsl:call-template name="option">
              <xsl:with-param name="tested"><xsl:value-of select="$field"/></xsl:with-param>
              <xsl:with-param name="key">ipaddress</xsl:with-param>
              <xsl:with-param name="text">IP Address</xsl:with-param>
            </xsl:call-template>
            <xsl:call-template name="option">
              <xsl:with-param name="tested"><xsl:value-of select="$field"/></xsl:with-param>
              <xsl:with-param name="key">timestamp</xsl:with-param>
              <xsl:with-param name="text">Login Time</xsl:with-param>
            </xsl:call-template>
          </select>
        </td>
        <td>
          <select name="operator{$id}">
            <xsl:call-template name="option">
              <xsl:with-param name="tested"><xsl:value-of select="$operator"/></xsl:with-param>
              <xsl:with-param name="key">none</xsl:with-param>
              <xsl:with-param name="text">(operator)</xsl:with-param>
            </xsl:call-template>
            <xsl:call-template name="option">
              <xsl:with-param name="tested"><xsl:value-of select="$operator"/></xsl:with-param>
              <xsl:with-param name="key">equal</xsl:with-param>
              <xsl:with-param name="text">equals</xsl:with-param>
            </xsl:call-template>
            <xsl:call-template name="option">
              <xsl:with-param name="tested"><xsl:value-of select="$operator"/></xsl:with-param>
              <xsl:with-param name="key">greater</xsl:with-param>
              <xsl:with-param name="text">greater than</xsl:with-param>
            </xsl:call-template>
            <xsl:call-template name="option">
              <xsl:with-param name="tested"><xsl:value-of select="$operator"/></xsl:with-param>
              <xsl:with-param name="key">less</xsl:with-param>
              <xsl:with-param name="text">less than</xsl:with-param>
            </xsl:call-template>
          </select>
        </td>
        <td><input class="text" type="text" name="value{$id}" value="{$value}"/></td>
      </tr>
    </table>
  </xsl:template>

  <xsl:template name="pages">
    <tr>
      <td>
        <table width="100%">
        <tr>
          <td align="left" width="33%">
            <xsl:if test="current &gt; 1">
              <form method="post" action="main.do">
              <input type="hidden" name="page" value="{key}"/>
              <input type="hidden" name="current" value="{current - 1}"/>
              <input class="button" type="submit" value="Previous"/>
              </form>
            </xsl:if>
          </td>
          <td align="center" width="33%">
            <form method="post" action="main.do">
            Pages:
            <select name="current">
              <xsl:call-template name="recursion">
                <xsl:with-param name="i">1</xsl:with-param>
                <xsl:with-param name="n"><xsl:value-of select="pages"/></xsl:with-param>
                <xsl:with-param name="selected"><xsl:value-of select="current"/></xsl:with-param>
              </xsl:call-template>
            </select>
            <input type="hidden" name="page" value="{key}"/>
            <input class="button" type="submit" value="Go"/>
            </form>
          </td>
          <td align="right" width="34%">
            <xsl:if test="current &lt; pages">
              <form method="post" action="main.do">
              <input type="hidden" name="page" value="{key}"/>
              <input type="hidden" name="current" value="{current + 1}"/>
              <input class="button" type="submit" value="Next"/>
              </form>
            </xsl:if>
          </td>
        </tr>
        </table>
      </td>
    </tr>
  </xsl:template>

  <xsl:template name="recursion">
    <xsl:param name="i"/>
    <xsl:param name="n"/>
    <xsl:param name="selected"/>
    <xsl:call-template name="option">
      <xsl:with-param name="tested"><xsl:value-of select="$selected"/></xsl:with-param>
      <xsl:with-param name="key"><xsl:value-of select="$i"/></xsl:with-param>
      <xsl:with-param name="text"><xsl:value-of select="$i"/></xsl:with-param>
    </xsl:call-template>
    <xsl:if test="$i &lt; $n">
      <xsl:call-template name="recursion">
        <xsl:with-param name="i"><xsl:value-of select="$i + 1"/></xsl:with-param>
        <xsl:with-param name="n"><xsl:value-of select="$n"/></xsl:with-param>
        <xsl:with-param name="selected"><xsl:value-of select="$selected"/></xsl:with-param>
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

  <xsl:template name="option">
    <xsl:param name="tested"/>
    <xsl:param name="key"/>
    <xsl:param name="text"/>
    <xsl:choose>
      <xsl:when test="$tested = $key">
        <option value="{$key}" selected="true"><xsl:value-of select="$text"/></option>
      </xsl:when>
      <xsl:otherwise>
        <option value="{$key}"><xsl:value-of select="$text"/></option>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="keytotext">
    <xsl:param name="key"/>
    <xsl:choose>
      <xsl:when test="$key = 'username'">Username</xsl:when>
      <xsl:when test="$key = 'dbname'">Database</xsl:when>
      <xsl:when test="$key = 'ipaddress'">IP Address</xsl:when>
      <xsl:when test="$key = 'result'">Result</xsl:when>
      <xsl:when test="$key = 'timestamp'">Login Time</xsl:when>
      <xsl:when test="$key = 'minute'">Login Time (Minute)</xsl:when>
      <xsl:when test="$key = 'hour'">Login Time (Hour)</xsl:when>
      <xsl:when test="$key = 'day'">Login Time (Day)</xsl:when>
      <xsl:when test="$key = 'month'">Login Time (Month)</xsl:when>
      <xsl:when test="$key = 'year'">Login Time (Year)</xsl:when>
    </xsl:choose>
  </xsl:template>
</xsl:stylesheet>
