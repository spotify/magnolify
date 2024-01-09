$!
  Copyright (c) 2016-2018 Martin Donath <martin.donath@squidfunk.com>
  Copyright (c) 2017-2018 Jonas Fonseca <jonas.fonseca@gmail.com>

  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to
  deal in the Software without restriction, including without limitation the
  rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
  sell copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in
  all copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
  IN THE SOFTWARE.
!$
<header class="md-header" data-md-component="header">
  <nav class="md-header-nav md-grid">
    <div class="md-flex">
      <div class="md-flex__cell md-flex__cell--shrink">
        <a href="$page.home.href$" title="$page.home.title$" class="md-header-nav__button md-logo">
          $ if (page.properties.("material.logo.icon")) $
            <i class="md-icon">$page.properties.("material.logo.icon")$</i>
          $ else $
            $ if (page.properties.("material.logo.uri")) $
              <img src="$page.properties.("material.logo.uri")$" width="24" height="24">
            $ else $
              <img src="$page.base$$page.properties.("material.logo")$" width="24" height="24">
            $endif$
          $endif$
        </a>
      </div>
      <div class="md-flex__cell md-flex__cell--shrink">
        <label class="md-icon md-icon--menu md-header-nav__button" for="__drawer"></label>
      </div>
      <div class="md-flex__cell md-flex__cell--stretch">
        <div class="md-flex__ellipsis md-header-nav__title" data-md-component="title">
          $ if (page.home.active) $
            $page.home.title$
          $ else $
            <span class="md-header-nav__topic">
              $page.home.title$
            </span>
            <span class="md-header-nav__topic">
              $page.title$
            </span>
          $ endif $
        </div>
      </div>
      $ if (page.properties.("material.search")) $
        <div class="md-flex__cell md-flex__cell--shrink">
          <label class="md-icon md-icon--search md-header-nav__button" for="__search"></label>
          $search()$
        </div>
      $ else $
        <form name="search" style="display: none">
          <input type="text" name="query">
        </form>
      $ endif $
      $ if (page.properties.("material.repo")) $
        <div class="md-flex__cell md-flex__cell--shrink">
          <div class="md-header-nav__source">
            $source()$
          </div>
        </div>
      $ endif $
    </div>
  </nav>
</header>
