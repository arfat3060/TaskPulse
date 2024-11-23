using System.Data;
using TaskPulse.Interfaces;
using TaskPulse.Model;

namespace TaskPulse.Services;

public class UserRepository: IUserRepository
{
    private ILogger<UserRepository> _logger;
    public UserRepository(ILogger<UserRepository> logger)
    {
        _logger = logger;
    }
    public async Task<IEnumerable<TaskInfo>> GetAllUserDataAsync()
    {
        try
        {

            DataTable dt = new DataTable();
            dt.Columns.Add(new DataColumn("UserId", typeof(string)));
           

        }
        catch (Exception ex)
        {
            _logger.LogError(ex, ex.Message);
        }
    }
}