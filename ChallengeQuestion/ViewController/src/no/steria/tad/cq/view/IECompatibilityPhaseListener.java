package no.steria.tad.cq.view;

import javax.faces.context.ExternalContext; 
import javax.faces.context.FacesContext; 
import javax.faces.event.PhaseEvent; 
import javax.faces.event.PhaseId; 
import javax.faces.event.PhaseListener; 
import javax.servlet.http.HttpServletResponse; 
import javax.servlet.http.HttpSession; 
import org.apache.myfaces.trinidad.context.Agent; 
import org.apache.myfaces.trinidad.context.RequestContext; 
public class IECompatibilityPhaseListener implements PhaseListener {

    @SuppressWarnings("compatibility:2362218079027539834")
    private static final long serialVersionUID = -1925703009788595205L;

    public IECompatibilityPhaseListener() {
        super();
    }

    public void afterPhase(PhaseEvent phaseEvent) {
    }

    public void beforePhase(PhaseEvent phaseEvent) {
      FacesContext fctx = FacesContext.getCurrentInstance(); 
      ExternalContext ectx = fctx.getExternalContext(); 
      HttpServletResponse response = (HttpServletResponse) ectx.getResponse(); 
      response.addHeader("X-UA-Compatible", "IE=8"); 
    }

    public PhaseId getPhaseId() {
        return PhaseId.RENDER_RESPONSE;
    }
}