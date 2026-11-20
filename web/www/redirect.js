// www.qulaykurs.uz: sends every address to the same address on https://qulaykurs.uz, so search engines see one site.
export default {
  fetch(request) {
    const url = new URL(request.url);
    url.protocol = "https:";
    url.hostname = "qulaykurs.uz";
    url.port = "";
    return Response.redirect(url.toString(), 301);
  },
};
