package local.promptmark.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.jsp.jstl.core.Config;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LocaleFilterTest {

    @Test
    void propagates_request_locale_via_Config_set() throws Exception {
        LocaleFilter filter = new LocaleFilter();
        ServletRequest req = mock(ServletRequest.class);
        ServletResponse res = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getLocale()).thenReturn(Locale.KOREAN);

        filter.doFilter(req, res, chain);

        // JSTL's Config.set(ServletRequest, ...) writes a request attribute
        // whose name is derived from the config key (it appends ".request").
        // We don't lock onto the exact suffix — just that an attribute was set
        // whose name contains the FMT_LOCALE key, and the value is our Locale.
        ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> value = ArgumentCaptor.forClass(Object.class);
        verify(req, atLeastOnce()).setAttribute(name.capture(), value.capture());
        boolean matched = false;
        for (int i = 0; i < name.getAllValues().size(); i++) {
            String n = name.getAllValues().get(i);
            Object v = value.getAllValues().get(i);
            if (n != null && n.startsWith(Config.FMT_LOCALE) && Locale.KOREAN.equals(v)) {
                matched = true;
                break;
            }
        }
        org.assertj.core.api.Assertions.assertThat(matched)
            .as("LocaleFilter must call Config.set with the request locale")
            .isTrue();
        verify(chain).doFilter(req, res);
    }

    @Test
    void null_locale_does_not_set_attribute_but_chains() throws Exception {
        LocaleFilter filter = new LocaleFilter();
        ServletRequest req = mock(ServletRequest.class);
        ServletResponse res = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getLocale()).thenReturn(null);

        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
        // No locale attribute should have been written.
        verify(req, never()).setAttribute(contains(Config.FMT_LOCALE), any());
    }
}
