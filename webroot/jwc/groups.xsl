<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:template match="/groups">
  <html>
  <head>
    <link rel="stylesheet" type="text/css" href="/style.css"/>
    <title>JSnap Web Console</title>
    <script type="text/javascript" src="/jwc/commons.js"/>
    <script type="text/javascript">
    &lt;!--
    function showRename(group) {
      visible('inputrename' + group);
      visible('donerename' + group);
      visible('cancelrename' + group);
      setFocus('inputrename' + group);
    }

    function cancelRename(group) {
      invisible('inputrename' + group);
      invisible('donerename' + group);
      invisible('cancelrename' + group);
    }

    function doRename(group, name) {
      invisible('inputrename' + group);
      invisible('donerename' + group);
      invisible('cancelrename' + group);
      document.renamegroup.groupid.value = group;
      document.renamegroup.newname.value = name;
      document.renamegroup.submit();
    }

    function keypressed(event, group, name) {
      var key = (event.keyCode ? event.keyCode : (event.which ? event.which : event.charCode));
      if (key == 13)
        doRename(group, name);
      else if (key == 27)
        cancelRename(group);
    }

    function confirmGroupDelete(groupname) {
      return confirm('Group ' + groupname + ' will be deleted.\r\nGroup memberships will be lost.\r\nAre you sure?');
    }

    function init() {
      <xsl:choose>
        <xsl:when test="error[@at='create'] != ''">
          invisible('createlink');
        </xsl:when>
        <xsl:otherwise>
          invisible('createtable');
        </xsl:otherwise>
      </xsl:choose>
	  <xsl:for-each select="group">
	    invisible('inputrename<xsl:value-of select="id"/>');
	    invisible('donerename<xsl:value-of select="id"/>');
	    invisible('cancelrename<xsl:value-of select="id"/>');
	  </xsl:for-each>
    }
    // --&gt;
    </script>
  </head>
  <body onload="init();">
    <form method="post" action="main.do">
    <input type="hidden" name="page" value="{key}"/>
    <input type="hidden" name="create" value="true"/>
    <table class="darkgray" width="100%" cellspacing="0" cellpadding="4">
    <tr>
      <td>
      <table id="createlink">
        <tr><td><a href="#" onclick="visible('createtable');invisible('createlink');return false;"><small>Create a new group</small></a></td></tr>
      </table>
      <table id="createtable">
      <xsl:if test="error[@at='create'] != ''">
        <tr><td class="error" colspan="3"><xsl:value-of select="error[@at='create']"/></td></tr>
      </xsl:if>
      <tr>
        <td><b>Group Name:</b></td>
        <td><input type="text" name="newname" value="{newname}"/></td>
        <td><input class="button" type="submit" value="Create"/></td>
      </tr>
      <tr>
        <td><a id="hidelink" href="#" onclick="invisible('createtable');visible('createlink');return false;"><small>Hide</small></a></td>
        <td colspan="2"><input class="checkbox" type="checkbox" name="administrative"/><label>Members are administrators</label></td>
      </tr>
      </table>
      </td>
    </tr>
    </table>
    </form>
    <form method="post" action="main.do">
    <input type="hidden" name="page" value="{key}"/>
    <input type="hidden" name="refresh" value="true"/>
    <table class="lightgray" width="100%" cellspacing="0" cellpadding="4">
      <tr><td>
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
          <td><b>Group name:</b></td>
          <td><input class="text" type="text" name="groupname" value="{groupname}"/></td>
          <td><input class="button" type="submit" value="Search"/></td>
        </tr>
        <tr><td></td><td colspan="2"><i>Use wildcard (*) if necessary</i></td></tr>
      </table>
      </td></tr>
      <tr><td align="right" bgcolor="#ffffff">Reported on: <i><xsl:value-of select="now"/></i></td></tr>
    </table>
    </form>
    <table cellspacing="0" cellpadding="4">
      <xsl:for-each select="error[@at!='create']">
        <tr><td class="error"><xsl:value-of select="."/></td></tr>
      </xsl:for-each>
      <xsl:if test="message != ''">
        <tr><td class="notify"><xsl:value-of select="message"/></td></tr>
      </xsl:if>
    </table>
    <form name="renamegroup" method="post" action="main.do">
    <input type="hidden" name="page" value="groups"/>
    <input type="hidden" name="rename" value="true"/>
    <input type="hidden" name="groupid" value=""/>
    <input type="hidden" name="newname" value=""/>
    <table cellspacing="0" cellpadding="4">
      <xsl:choose>
        <xsl:when test="total &gt; 0">
          <xsl:choose>
            <xsl:when test="total = 1">
              <tr><td colspan="9">Found a single group.</td></tr>
            </xsl:when>
            <xsl:otherwise>
              <tr><td colspan="9">Found <xsl:value-of select="total"/> groups.</td></tr>
            </xsl:otherwise>
          </xsl:choose>
          <tr>
            <td><b>Group name</b></td>
            <td align="center"><b>Administrative</b></td>
            <td colspan="7"></td>
          </tr>
          <xsl:for-each select="group">
          <xsl:sort select="administrative" order="descending"/>
          <xsl:sort select="name"/>
            <tr>
              <td><xsl:value-of select="name"/></td>
              <td align="center"><xsl:value-of select="administrative"/></td>
              <td align="center"><a href="/jwc/main.do?page=users&amp;refresh=true&amp;groupstate=on&amp;group={id}"><small>[members - <xsl:value-of select="members"/>]</small></a></td>
              <td align="center"><a href="/jwc/main.do?page={../key}&amp;delete=true&amp;groupid={id}" onclick="return confirmGroupDelete('{name}');"><small>[delete]</small></a></td>
              <td align="center"><a href="/jwc/main.do?page={../key}&amp;invert=true&amp;groupid={id}"><small>[invert privileges]</small></a></td>
              <td align="center"><a href="#" onclick="showRename('{id}');return false;"><small>[rename]</small></a></td>
              <td><input id="inputrename{id}" class="inline" type="text" name="newname{id}" value="{name}" onkeypress="keypressed(event, '{id}', document.renamegroup.newname{id}.value);"/></td>
              <td><a id="donerename{id}" href="#" onclick="doRename('{id}', document.renamegroup.newname{id}.value);"><small>[done]</small></a></td>
              <td><a id="cancelrename{id}" href="#" onclick="cancelRename('{id}');return false;"><small>[cancel]</small></a></td>
            </tr>
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
          <tr><td>No groups matched the search criteria.</td></tr>
        </xsl:otherwise>
      </xsl:choose>
    </table>
    </form>
    <xsl:if test="pages &gt; 1">
      <xsl:call-template name="pages"/>
    </xsl:if>
  </body>
  </html>
  </xsl:template>

  <xsl:template name="pages">
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
</xsl:stylesheet>
