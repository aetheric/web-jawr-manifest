package bravura.sonata.web.common.jawr;

import lombok.extern.slf4j.Slf4j;
import net.jawr.web.JawrConstant;
import net.jawr.web.resource.bundle.JoinableResourceBundle;
import net.jawr.web.resource.bundle.handler.ResourceBundlesHandler;
import net.jawr.web.resource.bundle.iterator.BundlePath;
import org.apache.commons.codec.digest.DigestUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

/**
 * Generates an html5 cache manifest from JAWR cache information.
 * <p>Author: <a href="http://gplus.to/tzrlk">Peter Cummuskey</a></p>
 */
@Slf4j
public class JawrManifestServlet extends HttpServlet {

	public static final String HEADER_CACHE_KEY = "cache-control";
	public static final String HEADER_CACHE_VALUE = "public no-cache no-store max-age=0 must-revalidate no-transform";
	public static final String HEADER_ETAG_KEY = "etag";
	public static final String CONTENT_TYPE = "text/cache-manifest";

	protected static final String[] styleBundles = new String[] {
		"/bundles/ssapp.css"
	};

	protected static final String[] scriptBundles = new String[] {
		"/bundles/bravura_common_libs.js",
		"/bundles/bravura_common.js",
		"/bundles/bravura_angular_libs.js",
		"/bundles/bravura_angular.js",
		"/bundles/jsapp.js"
	};

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {

		// Generate and set the manifest content.
		final String manifest = generateManifest(request);
		final String etag = DigestUtils.md5Hex(manifest);
		response.getWriter().print(manifest);

		// Set all the caching headers, etc.
		response.setHeader(HEADER_CACHE_KEY, HEADER_CACHE_VALUE);
		response.setContentType(CONTENT_TYPE);
		response.setHeader(HEADER_ETAG_KEY, etag);
	}

	protected String generateManifest(HttpServletRequest request) {
		StringBuilder builder = new StringBuilder()
				.append("CACHE MANIFEST\n");

		builder.append("\nCACHE:\n");

		String styles = renderStyleBundles(request);
		builder.append(styles);

		String scripts = renderScriptBundles(request);
		builder.append(scripts);

		builder.append("\nNETWORK:\n");
		builder.append("*\n");

		return builder.toString();
	}

	protected String renderStyleBundles(HttpServletRequest request) {
		ResourceBundlesHandler handler = getHandler(request, JawrConstant.CSS_CONTEXT_ATTRIBUTE);
		if (handler == null) {
			return "<!-- Couldn't get style handler from servlet context -->";
		}

		return renderBundleLinks(handler, styleBundles);
	}

	protected String renderScriptBundles(HttpServletRequest request) {
		ResourceBundlesHandler handler = getHandler(request, JawrConstant.JS_CONTEXT_ATTRIBUTE);
		if (handler == null) {
			return "<!-- Couldn't get script handler from servlet context -->";
		}

		return renderBundleLinks(handler, scriptBundles);
	}

	protected ResourceBundlesHandler getHandler(HttpServletRequest request, String key) {
		return (ResourceBundlesHandler) request.getServletContext().getAttribute(key);
	}

	protected String renderBundleLinks(ResourceBundlesHandler handler, String[] bundles) {
		StringWriter writer = new StringWriter();

		for (String bundleName : bundles) {
			JoinableResourceBundle bundle = handler.resolveBundleForPath(bundleName);
			List<BundlePath> bundleItems = handler.getConfig().isDebugModeOn()
					? bundle.getItemDebugPathList()
					: bundle.getItemPathList();

			for (BundlePath bundleItem : bundleItems) {
				String itemPath = bundleItem.getPath();
				String itemHash = String.valueOf(bundleItem.hashCode());
				writer.append(itemPath).append(" # ").append(itemHash);
			}
		}

		return writer.toString();
	}

}
